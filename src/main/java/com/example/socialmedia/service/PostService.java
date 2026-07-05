package com.example.socialmedia.service;

import com.example.socialmedia.dto.PostRequest;
import com.example.socialmedia.dto.PostResponse;
import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.Role;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.Notification.NotificationType;
import com.example.socialmedia.repository.PostRepository;
import com.example.socialmedia.repository.UserRepository;
import com.example.socialmedia.repository.LikeRepository;
import com.example.socialmedia.repository.CommentRepository;
import com.example.socialmedia.entity.Like;
import com.example.socialmedia.entity.Comment;
import com.example.socialmedia.dto.CommentRequest;
import com.example.socialmedia.dto.CommentResponse;
import com.example.socialmedia.service.kafka.EventProducerService;
import com.example.socialmedia.dto.kafka.NotificationEvent;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

@Service
@CacheConfig(cacheNames = "posts")
public class PostService {
        private static final Logger log =
        LoggerFactory.getLogger(PostService.class);

        private final PostRepository postRepository;
        private final UserRepository userRepository;
        private final LikeRepository likeRepository;
        private final CommentRepository commentRepository;
        private final ExternalApiService externalApiService;
        private final NotificationService notificationService;
        private final com.example.socialmedia.repository.FollowRepository followRepository;
        private final com.example.socialmedia.repository.BlockRepository blockRepository;
        private final EventProducerService eventProducerService;

        public PostService(PostRepository postRepository, UserRepository userRepository,
                        LikeRepository likeRepository, CommentRepository commentRepository,
                        ExternalApiService externalApiService, NotificationService notificationService,
                        com.example.socialmedia.repository.FollowRepository followRepository,
                        com.example.socialmedia.repository.BlockRepository blockRepository,
                        EventProducerService eventProducerService) {
                this.postRepository = postRepository;
                this.userRepository = userRepository;
                this.likeRepository = likeRepository;
                this.commentRepository = commentRepository;
                this.externalApiService = externalApiService;
                this.notificationService = notificationService;
                this.followRepository = followRepository;
                this.blockRepository = blockRepository;
                this.eventProducerService = eventProducerService;
        }

        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "posts", allEntries = true),
                @CacheEvict(value = "feed", allEntries = true),
                @CacheEvict(value = "userProfiles", allEntries = true)
        })
        public PostResponse createPost(PostRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Post post = Post.builder()
                                .content(request.getContent())
                                .imageUrl(request.getImageUrl())
                                .user(user)
                                .build();

                Post savedPost = postRepository.save(post);

                externalApiService.notifyPostCreated(savedPost);

                // Send Kafka event for async processing
                sendNotificationEvent(post.getUser().getId(), "POST", "LIKE",
                        user.getId(), savedPost.getId(), "Post created");

                return mapToPostResponse(savedPost, userEmail);
        }

        @Transactional(readOnly = true)
        @Cacheable(value = "feed", key = "'user:' + #currentUserEmail")
        public Page<PostResponse> getHomeFeed(String currentUserEmail, Pageable pageable) {
                User user = userRepository.findByEmail(currentUserEmail)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get followings
                var followings = followRepository.findByFollowerId(user.getId())
                                .stream().map(f -> f.getFollowing().getId()).toList();

                // Include self in feed
                followings.add(user.getId());

                return postRepository.findByUserIdIn(followings, pageable)
                                .map(post -> mapToPostResponse(post, currentUserEmail));
        }

        @Transactional(readOnly = true)
        public Page<PostResponse> getAllPosts(Pageable pageable, String currentUserEmail) {
                return postRepository.findAllPublicPosts(pageable)
                                .map(post -> mapToPostResponse(post, currentUserEmail));
        }

        @Transactional(readOnly = true)
        public Page<PostResponse> getPostsByUser(Long userId, Pageable pageable,
                        String viewerEmail) {
                java.util.Objects.requireNonNull(userId, "User ID cannot be null");
                User targetUser = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                User viewer = userRepository.findByEmail(viewerEmail)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // 1. Block Check
                if (blockRepository.existsByBlockerAndBlocked(targetUser, viewer) ||
                                blockRepository.existsByBlockerAndBlocked(viewer, targetUser)) {
                        throw new RuntimeException("Content invalid or unavailable");
                }

                // 2. Private Check
                if (targetUser.isPrivateAccount() && !targetUser.getId().equals(viewer.getId())) {
                        boolean isFollowing = followRepository.existsByFollowerAndFollowing(viewer, targetUser);
                        if (!isFollowing) {
                                throw new RuntimeException("This account is private");
                        }
                }

                return postRepository.findByUserId(userId, pageable)
                                .map(post -> mapToPostResponse(post, viewerEmail));
        }

        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "feed", allEntries = true),
                @CacheEvict(value = "posts", allEntries = true)
        })
        public String toggleLike(Long postId, String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new RuntimeException("Post not found"));

                java.util.Optional<Like> existingLike = likeRepository.findByUserAndPost(user, post);

                if (existingLike.isPresent()) {
                        likeRepository.delete(existingLike.get());
                        return "Post unliked";
                } else {
                        Like like = new Like();
                        like.setUser(user);
                        like.setPost(post);
                        likeRepository.save(like);

                        // Send async notification via Kafka (non-blocking)
                        sendNotificationEvent(
                                post.getUser().getId(),
                                "POST",
                                "LIKE",
                                user.getId(),
                                postId,
                                getDisplayName(user) + " liked your post"
                        );

                        return "Post liked";
                }
        }

        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "feed", allEntries = true),
                @CacheEvict(value = "posts", allEntries = true)
        })
        public void addComment(Long postId, CommentRequest request, String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new RuntimeException("Post not found"));

                Comment comment = new Comment();
                comment.setContent(request.getContent());
                comment.setUser(user);
                comment.setPost(post);

                commentRepository.save(comment);

                // Send async notification via Kafka (non-blocking)
                sendNotificationEvent(
                        post.getUser().getId(),
                        "POST",
                        "COMMENT",
                        user.getId(),
                        postId,
                        getDisplayName(user) + " commented on your post"
                );
        }

        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "posts", allEntries = true),
                @CacheEvict(value = "feed", allEntries = true),
                @CacheEvict(value = "userProfiles", allEntries = true)
        })
        public void deletePost(Long postId, String email) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new RuntimeException("Post not found"));

                if (!post.getUser().getEmail().equals(email) && !isAdmin(email)) {
                        throw new RuntimeException("Unauthorized");
                }

                postRepository.delete(post);
        }

        @Transactional(readOnly = true)
        public Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable) {
                return commentRepository.findByPostId(postId, pageable)
                                .map(comment -> {
                                        String name = "Unknown";
                                        if (comment.getUser().getUserInfo() != null) {
                                                name = comment.getUser().getUserInfo().getFirstName() + " "
                                                                + comment.getUser().getUserInfo().getLastName();
                                        } else {
                                                name = comment.getUser().getEmail();
                                        }
                                        return new CommentResponse(
                                                        comment.getId(),
                                                        comment.getContent(),
                                                        name,
                                                        comment.getUser().getId(),
                                                        comment.getCreatedAt());
                                });
        }

        @Transactional
        public void deleteComment(Long commentId, String email) {
                Comment comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new RuntimeException("Comment not found"));

                if (!comment.getUser().getEmail().equals(email)) {
                        throw new RuntimeException("You can only delete your own comments");
                }

                commentRepository.delete(comment);
        }

        private boolean isAdmin(String email) {
                return userRepository.findByEmail(email)
                                .map(user -> user.getRole() == Role.ROLE_ADMIN)
                                .orElse(false);
        }

        private String getDisplayName(User user) {
                if (user.getUserInfo() != null && user.getUserInfo().getFirstName() != null) {
                        return user.getUserInfo().getFirstName() + " " +
                                        (user.getUserInfo().getLastName() != null ? user.getUserInfo().getLastName()
                                                        : "");
                }
                return user.getEmail().split("@")[0];
        }

        private PostResponse mapToPostResponse(Post post, String currentUserEmail) {
                boolean isLiked = likeRepository.existsByUserEmailAndPostId(currentUserEmail, post.getId());
                return PostResponse.builder()
                                .id(post.getId())
                                .content(post.getContent())
                                .imageUrl(post.getImageUrl())
                                .authorName(post.getUser().getUserInfo() != null
                                                ? post.getUser().getUserInfo().getFirstName() + " "
                                                                + post.getUser().getUserInfo().getLastName()
                                                : post.getUser().getEmail())
                                .authorId(post.getUser().getId())
                                .likeCount(post.getLikes() != null ? post.getLikes().size() : 0)
                                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                                .createdAt(post.getCreatedAt())
                                .isLiked(isLiked)
                                .build();
        }

        // Helper to send async notification via Kafka
        private void sendNotificationEvent(Long targetUserId, String resourceType,
                        String type, Long actorId, Long resourceId, String content) {
                try {
                    eventProducerService.sendNotification(new NotificationEvent(
                            type, actorId, targetUserId, resourceId, resourceType, content, Instant.now()
                    ));
                } catch (Exception e) {
                        // Log but don't fail - Kafka is optional for notifications
                        log.warn("Failed to send async notification: {}", e.getMessage());
                }
        }
}
