package com.example.socialmedia.service;

import com.example.socialmedia.dto.FollowUserResponse;
import com.example.socialmedia.dto.NetworkGraphResponse;
import com.example.socialmedia.dto.RecommendationResponse;
import com.example.socialmedia.dto.SearchUserResponse;
import com.example.socialmedia.dto.UserProfileRequest;
import com.example.socialmedia.dto.UserProfileResponse;
import com.example.socialmedia.entity.*;
import com.example.socialmedia.entity.Notification.NotificationType;
import com.example.socialmedia.repository.*;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final BlockRepository blockRepository;
    private final NotificationService notificationService;
    private final SupabaseStorageService storageService;

    public UserService(UserRepository userRepository, UserInfoRepository userInfoRepository,
            FollowRepository followRepository, FollowRequestRepository followRequestRepository,
            BlockRepository blockRepository, NotificationService notificationService,
            SupabaseStorageService storageService) {
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
        this.followRepository = followRepository;
        this.followRequestRepository = followRequestRepository;
        this.blockRepository = blockRepository;
        this.notificationService = notificationService;
        this.storageService = storageService;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // ─── Profile ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Cacheable(value = "userProfiles", key = "#email")
    public UserProfileResponse getProfile(String email) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        return buildProfileResponse(user, info, user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userProfiles", key = "#targetId")
    public UserProfileResponse getOtherProfile(Long targetId, String requesterEmail) {
        User requester = getUserByEmail(requesterEmail);
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserInfo info = userInfoRepository.findByUser(target).orElse(null);
        return buildProfileResponse(target, info, requester);
    }

    @Transactional
    @CacheEvict(value = "userProfiles", key = "#email", beforeInvocation = false)
    public UserProfileResponse updateProfile(String email, UserProfileRequest request) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);

        if (info == null) {
            info = new UserInfo();
            info.setUser(user);
        }

        info.setFirstName(request.getFirstName());
        info.setLastName(request.getLastName());
        info.setBio(request.getBio());
        info.setProfilePicUrl(request.getProfilePicUrl());
        if (request.getWebsite() != null) info.setWebsite(request.getWebsite());
        if (request.getLocation() != null) info.setLocation(request.getLocation());
        if (request.getGender() != null) info.setGender(request.getGender());
        if (request.getDob() != null) info.setDob(request.getDob());
        if (request.getPhone() != null) info.setPhone(request.getPhone());
        if (request.getProfileVisibility() != null) {
            try {
                info.setProfileVisibility(ProfileVisibility.valueOf(request.getProfileVisibility()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        userInfoRepository.save(info);
        return buildProfileResponse(user, info, user);
    }

    @Transactional
    @CacheEvict(value = "userProfiles", key = "#email", beforeInvocation = false)
    public UserProfileResponse updateProfilePic(String email, String profilePicUrl) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        if (info == null) {
            info = new UserInfo();
            info.setUser(user);
        }
        if (info.getProfilePicUrl() != null) {
            storageService.deleteImage(info.getProfilePicUrl());
        }
        info.setProfilePicUrl(profilePicUrl);
        userInfoRepository.save(info);
        return buildProfileResponse(user, info, user);
    }

    @Transactional
    @CacheEvict(value = "userProfiles", key = "#email", beforeInvocation = false)
    public UserProfileResponse removeProfilePic(String email) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        if (info != null && info.getProfilePicUrl() != null) {
            storageService.deleteImage(info.getProfilePicUrl());
            info.setProfilePicUrl(null);
            userInfoRepository.save(info);
        }
        return buildProfileResponse(user, info, user);
    }

    @Transactional
    @CacheEvict(value = "userProfiles", key = "#email", beforeInvocation = false)
    public UserProfileResponse updateProfileSettings(String email, UserProfileRequest request) {
        return updateProfile(email, request);
    }

    private UserProfileResponse buildProfileResponse(User user, UserInfo info, User requester) {
        boolean isMe = user.getId().equals(requester.getId());
        boolean isFollowing = followRepository.findByFollowerAndFollowing(requester, user).isPresent();
        boolean isFollowRequested = followRequestRepository.findByRequesterAndTarget(requester, user).isPresent();
        boolean canViewPosts = isMe || !user.isPrivateAccount() || isFollowing;

        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(info != null ? info.getFirstName() : null)
                .lastName(info != null ? info.getLastName() : null)
                .bio(info != null ? info.getBio() : null)
                .profilePicUrl(info != null ? info.getProfilePicUrl() : null)
                .followerCount(followRepository.findByFollowingId(user.getId()).size())
                .followingCount(followRepository.findByFollowerId(user.getId()).size())
                .isPrivateAccount(user.isPrivateAccount())
                .isFollowing(isFollowing)
                .isFollowRequested(isFollowRequested)
                .canViewPosts(canViewPosts)
                .role(user.getRole())
                .build();
    }

    // ─── Follow / Unfollow ────────────────────────────────────

    @Transactional
    public String toggleFollow(Long targetUserId, String email) {
        User follower = getUserByEmail(email);
        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (follower.getId().equals(following.getId())) {
            throw new RuntimeException("You cannot follow yourself");
        }

        if (isBlocked(follower, following) || isBlocked(following, follower)) {
            throw new RuntimeException("Action not allowed");
        }

        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowing(follower, following);

        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            return "Unfollowed successfully";
        }

        if (following.isPrivateAccount()) {
            Optional<FollowRequest> existing = followRequestRepository.findByRequesterAndTarget(follower, following);
            if (existing.isPresent()) {
                if (existing.get().getStatus() == FollowRequestStatus.PENDING) {
                    return "Follow request already sent";
                }
                followRequestRepository.delete(existing.get());
            }
            FollowRequest fr = new FollowRequest(follower, following);
            followRequestRepository.save(fr);
            notificationService.createNotification(
                    following, NotificationType.FOLLOW_REQUEST,
                    getDisplayName(follower) + " requested to follow you", follower);
            return "Follow request sent";
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);

        notificationService.createNotification(
                following, NotificationType.FOLLOW,
                getDisplayName(follower) + " started following you", follower);

        return "Followed successfully";
    }

    // ─── Follow Requests ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getPendingFollowRequests(String email) {
        User user = getUserByEmail(email);
        return followRequestRepository.findByTargetIdAndStatus(user.getId(), FollowRequestStatus.PENDING)
                .stream()
                .map(fr -> mapToFollowUserResponse(fr.getRequester()))
                .collect(Collectors.toList());
    }

    @Transactional
    public String acceptFollowRequest(Long requestId, String email) {
        User user = getUserByEmail(email);
        FollowRequest fr = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));
        if (!fr.getTarget().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }
        fr.setStatus(FollowRequestStatus.ACCEPTED);
        followRequestRepository.save(fr);

        Follow follow = new Follow();
        follow.setFollower(fr.getRequester());
        follow.setFollowing(user);
        followRepository.save(follow);

        notificationService.createNotification(
                fr.getRequester(), NotificationType.FOLLOW,
                getDisplayName(user) + " accepted your follow request", user);

        return "Follow request accepted";
    }

    @Transactional
    public String rejectFollowRequest(Long requestId, String email) {
        User user = getUserByEmail(email);
        FollowRequest fr = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));
        if (!fr.getTarget().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }
        fr.setStatus(FollowRequestStatus.REJECTED);
        followRequestRepository.delete(fr);
        return "Follow request rejected";
    }

    @Transactional
    public String acceptFollowRequestFromUser(Long requesterId, String email) {
        User user = getUserByEmail(email);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FollowRequest fr = followRequestRepository.findByRequesterAndTarget(requester, user)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));

        if (fr.getStatus() != FollowRequestStatus.PENDING) {
            return "Request already processed";
        }

        fr.setStatus(FollowRequestStatus.ACCEPTED);
        followRequestRepository.save(fr);

        Follow follow = new Follow();
        follow.setFollower(fr.getRequester());
        follow.setFollowing(user);
        followRepository.save(follow);

        notificationService.createNotification(
                fr.getRequester(), NotificationType.FOLLOW,
                getDisplayName(user) + " accepted your follow request", user);

        return "Follow request accepted";
    }

    @Transactional
    public String rejectFollowRequestFromUser(Long requesterId, String email) {
        User user = getUserByEmail(email);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FollowRequest fr = followRequestRepository.findByRequesterAndTarget(requester, user)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));

        followRequestRepository.delete(fr);
        return "Follow request rejected";
    }

    // ─── Privacy Toggle ───────────────────────────────────────

    @Transactional
    public Map<String, Object> togglePrivateAccount(String email) {
        User user = getUserByEmail(email);
        user.setPrivateAccount(!user.isPrivateAccount());
        userRepository.save(user);
        return Map.of("isPrivateAccount", user.isPrivateAccount());
    }

    // ─── Block / Unblock ──────────────────────────────────────

    @Transactional
    public String blockUser(Long targetUserId, String email) {
        User blocker = getUserByEmail(email);
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (blocker.getId().equals(blocked.getId())) {
            throw new RuntimeException("You cannot block yourself");
        }

        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            return "User already blocked";
        }

        blockRepository.save(new Block(blocker, blocked));

        followRepository.findByFollowerAndFollowing(blocker, blocked).ifPresent(followRepository::delete);
        followRepository.findByFollowerAndFollowing(blocked, blocker).ifPresent(followRepository::delete);

        return "User blocked";
    }

    @Transactional
    public String unblockUser(Long targetUserId, String email) {
        User blocker = getUserByEmail(email);
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        blockRepository.findByBlockerAndBlocked(blocker, blocked)
                .ifPresent(blockRepository::delete);

        return "User unblocked";
    }

    public boolean isBlocked(User a, User b) {
        return blockRepository.existsByBlockerAndBlocked(a, b);
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getBlockedUsers(String email) {
        User user = getUserByEmail(email);
        return blockRepository.findByBlockerId(user.getId()).stream()
                .map(b -> mapToFollowUserResponse(b.getBlocked()))
                .collect(Collectors.toList());
    }

    // ─── Followers / Following ────────────────────────────────

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowers(Long userId) {
        return followRepository.findByFollowingId(userId).stream()
                .map(follow -> mapToFollowUserResponse(follow.getFollower()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowing(Long userId) {
        return followRepository.findByFollowerId(userId).stream()
                .map(follow -> mapToFollowUserResponse(follow.getFollowing()))
                .collect(Collectors.toList());
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId).orElse(null);
        User following = userRepository.findById(followingId).orElse(null);
        if (follower == null || following == null) return false;
        return followRepository.findByFollowerAndFollowing(follower, following).isPresent();
    }

    // ─── Network Graph ────────────────────────────────────────

    @Transactional(readOnly = true)
    public NetworkGraphResponse getNetworkGraph(Long targetUserId, String requesterEmail) {
        User requester = getUserByEmail(requesterEmail);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean canView = target.getId().equals(requester.getId())
                || !target.isPrivateAccount()
                || followRepository.findByFollowerAndFollowing(requester, target).isPresent();
        if (!canView) throw new RuntimeException("Access denied");

        List<User> friends = followRepository.findByFollowerId(target.getId()).stream()
                .map(Follow::getFollowing)
                .collect(Collectors.toList());

        java.util.Set<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toSet());

        java.util.Map<Long, java.util.Set<Long>> mutualMap = new java.util.HashMap<>();
        for (User friend : friends) {
            followRepository.findByFollowerId(friend.getId()).forEach(f -> {
                Long mid = f.getFollowing().getId();
                if (!mid.equals(target.getId()) && !friendIds.contains(mid)) {
                    mutualMap.computeIfAbsent(mid, k -> new java.util.HashSet<>()).add(friend.getId());
                }
            });
        }

        List<NetworkGraphResponse.MutualNodeDTO> mutuals = mutualMap.entrySet().stream()
                .limit(20)
                .map(e -> {
                    User u = userRepository.findById(e.getKey()).orElse(null);
                    if (u == null) return null;
                    return new NetworkGraphResponse.MutualNodeDTO(
                            u.getId(), firstName(u), u.getEmail(), picUrl(u),
                            List.copyOf(e.getValue()));
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        NetworkGraphResponse.NodeDTO centerNode = new NetworkGraphResponse.NodeDTO(
                target.getId(), firstName(target), target.getEmail(), picUrl(target));

        List<NetworkGraphResponse.NodeDTO> friendNodes = friends.stream()
                .map(u -> new NetworkGraphResponse.NodeDTO(u.getId(), firstName(u), u.getEmail(), picUrl(u)))
                .collect(Collectors.toList());

        return new NetworkGraphResponse(centerNode, friendNodes, mutuals);
    }

    // ─── Smart Search ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SearchUserResponse> searchUsers(String requesterEmail, String query,
                                                String location, String bio,
                                                boolean mutualOnly, String sort) {
        User requester = getUserByEmail(requesterEmail);
        List<User> results = userRepository.searchUsers(
                requester.getId(), query, location, bio, PageRequest.of(0, 50));

        Set<Long> myFollowingIds = followRepository.findByFollowerId(requester.getId())
                .stream().map(f -> f.getFollowing().getId()).collect(Collectors.toSet());

        List<SearchUserResponse> mapped = results.stream().map(u -> {
            // count mutuals: people I follow who also follow this user
            long mutual = followRepository.findByFollowingId(u.getId()).stream()
                    .filter(f -> myFollowingIds.contains(f.getFollower().getId()))
                    .count();
            boolean following = myFollowingIds.contains(u.getId());
            String name = u.getUserInfo() != null
                    ? ((u.getUserInfo().getFirstName() != null ? u.getUserInfo().getFirstName() : "") +
                       " " + (u.getUserInfo().getLastName() != null ? u.getUserInfo().getLastName() : "")).trim()
                    : u.getEmail();
            String loc = u.getUserInfo() != null ? u.getUserInfo().getLocation() : null;
            String userBio = u.getUserInfo() != null ? u.getUserInfo().getBio() : null;
            String pic = u.getUserInfo() != null ? u.getUserInfo().getProfilePicUrl() : null;
            return new SearchUserResponse(u.getId(), name, u.getEmail(), pic, loc, userBio, (int) mutual, following);
        }).collect(Collectors.toList());

        if (mutualOnly) {
            mapped = mapped.stream().filter(r -> r.getMutualCount() > 0).collect(Collectors.toList());
        }

        if ("mutual".equals(sort)) {
            mapped.sort((a, b) -> Integer.compare(b.getMutualCount(), a.getMutualCount()));
        } else if ("name".equals(sort)) {
            mapped.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        }

        return mapped;
    }

    // ─── Recommendations ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(String email) {
        User me = getUserByEmail(email);
        Set<String> myKeywords = extractKeywords(me.getUserInfo() != null ? me.getUserInfo().getBio() : null);
        String myLocation = me.getUserInfo() != null ? me.getUserInfo().getLocation() : null;
        Set<Long> myFollowingIds = followRepository.findByFollowerId(me.getId())
                .stream().map(f -> f.getFollowing().getId()).collect(Collectors.toSet());
        List<User> candidates = userRepository.findRecommendationCandidates(me.getId(), PageRequest.of(0, 100));
        return candidates.stream()
                .map(u -> scoreUser(u, myKeywords, myLocation, myFollowingIds))
                .filter(r -> r.getMatchScore() > 0)
                .sorted((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private RecommendationResponse scoreUser(User u, Set<String> myKeywords, String myLocation,
                                             Set<Long> myFollowingIds) {
        int score = 0;
        java.util.List<String> reasons = new java.util.ArrayList<>();
        UserInfo info = u.getUserInfo();

        long mutuals = followRepository.findByFollowingId(u.getId()).stream()
                .filter(f -> myFollowingIds.contains(f.getFollower().getId())).count();
        if (mutuals > 0) {
            score += (int) Math.min(mutuals * 15, 45);
            reasons.add(mutuals + " mutual friend" + (mutuals > 1 ? "s" : ""));
        }

        String theirLoc = info != null ? info.getLocation() : null;
        if (myLocation != null && theirLoc != null && myLocation.trim().equalsIgnoreCase(theirLoc.trim())) {
            score += 25;
            reasons.add("Same location");
        }

        Set<String> sharedKeywords = extractKeywords(info != null ? info.getBio() : null);
        sharedKeywords.retainAll(myKeywords);
        if (!sharedKeywords.isEmpty()) {
            score += Math.min(sharedKeywords.size() * 10, 30);
            reasons.add("Shared interests: " + String.join(", ", sharedKeywords));
        }

        if (info != null && info.getProfilePicUrl() != null) score += 5;

        String name = info != null
                ? ((info.getFirstName() != null ? info.getFirstName() : "") + " "
                   + (info.getLastName() != null ? info.getLastName() : "")).trim()
                : u.getEmail();
        if (name.isBlank()) name = u.getEmail();

        return new RecommendationResponse(u.getId(), name, u.getEmail(),
                info != null ? info.getProfilePicUrl() : null,
                theirLoc, info != null ? info.getBio() : null,
                score, reasons);
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return new java.util.HashSet<>();
        return java.util.Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toSet());
    }

    // ─── Suggestions ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getSuggestedUsers(String email) {
        User user = getUserByEmail(email);
        return userRepository.findSuggestedUsers(user.getId(), PageRequest.of(0, 5))
                .stream()
                .map(this::mapToFollowUserResponse)
                .collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────────

    private String firstName(User u) {
        return u.getUserInfo() != null ? u.getUserInfo().getFirstName() : null;
    }

    private String picUrl(User u) {
        return u.getUserInfo() != null ? u.getUserInfo().getProfilePicUrl() : null;
    }

    private String getDisplayName(User user) {
        if (user.getUserInfo() != null && user.getUserInfo().getFirstName() != null) {
            return user.getUserInfo().getFirstName() + " " +
                    (user.getUserInfo().getLastName() != null ? user.getUserInfo().getLastName() : "");
        }
        return user.getEmail().split("@")[0];
    }

    private FollowUserResponse mapToFollowUserResponse(User user) {
        String name;
        String profilePicUrl = null;
        if (user.getUserInfo() != null) {
            name = user.getUserInfo().getFirstName() + " " + user.getUserInfo().getLastName();
            profilePicUrl = user.getUserInfo().getProfilePicUrl();
        } else {
            name = user.getEmail();
        }
        return new FollowUserResponse(user.getId(), name, profilePicUrl);
    }
}
