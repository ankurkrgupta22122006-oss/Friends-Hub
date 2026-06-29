package com.example.socialmedia.service;

import com.example.socialmedia.dto.FriendStatsResponse;
import com.example.socialmedia.dto.FriendStatsResponse.WeeklyPoint;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.repository.ChatMessageRepository;
import com.example.socialmedia.repository.CommentRepository;
import com.example.socialmedia.repository.FollowRepository;
import com.example.socialmedia.repository.LikeRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FriendStatsService {

    // DAYOFWEEK: 1=Sunday … 7=Saturday
    private static final String[] DAY_NAMES = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;

    public FriendStatsService(UserRepository userRepository,
                              ChatMessageRepository chatMessageRepository,
                              LikeRepository likeRepository,
                              CommentRepository commentRepository,
                              FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.followRepository = followRepository;
    }

    @Transactional(readOnly = true)
    public FriendStatsResponse getStats(Long friendId, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Access guard: must follow each other or be the same person
        boolean canView = requester.getId().equals(friend.getId())
                || followRepository.findByFollowerAndFollowing(requester, friend).isPresent()
                || followRepository.findByFollowerAndFollowing(friend, requester).isPresent();
        if (!canView) throw new RuntimeException("Access denied");

        long totalMessages = chatMessageRepository.countConversation(requester, friend);

        long likesGiven = likeRepository.countLikesByLikerOnOwnerPosts(requester, friend);
        long likesReceived = likeRepository.countLikesByLikerOnOwnerPosts(friend, requester);

        long commentsGiven = commentRepository.countCommentsByCommenterOnOwnerPosts(
                requester.getId(), friend.getId());
        long commentsReceived = commentRepository.countCommentsByCommenterOnOwnerPosts(
                friend.getId(), requester.getId());

        // Weekly message breakdown — last 7 days
        LocalDateTime since = LocalDateTime.now().minusDays(6).toLocalDate().atStartOfDay();
        List<Object[]> rawWeekly = chatMessageRepository.countByDayOfWeek(requester, friend, since);
        Map<Integer, Long> dayMap = rawWeekly.stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).intValue(), r -> ((Number) r[1]).longValue()));

        // Build 7-point series starting from today going back
        List<WeeklyPoint> weekly = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime day = LocalDateTime.now().minusDays(i);
            // DAYOFWEEK: Sunday=1
            int dow = day.getDayOfWeek().getValue() % 7 + 1; // ISO Mon=1 → Sun=1..Sat=7
            String label = DAY_NAMES[dow - 1];
            weekly.add(new WeeklyPoint(label, dayMap.getOrDefault(dow, 0L)));
        }

        return new FriendStatsResponse(totalMessages, likesGiven, likesReceived,
                commentsGiven, commentsReceived, weekly);
    }
}
