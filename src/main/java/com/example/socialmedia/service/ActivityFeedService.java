package com.example.socialmedia.service;

import com.example.socialmedia.dto.ActivityFeedItem;
import com.example.socialmedia.entity.Follow;
import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.repository.FollowRepository;
import com.example.socialmedia.repository.PostRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityFeedService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int LOOKBACK_DAYS = 30;

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;

    public ActivityFeedService(UserRepository userRepository,
                               FollowRepository followRepository,
                               PostRepository postRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedItem> getFeed(String email, int page, int size) {
        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Follow> myFollows = followRepository.findByFollowerId(me.getId());
        if (myFollows.isEmpty()) return List.of();

        List<Long> followingIds = myFollows.stream()
                .map(f -> f.getFollowing().getId())
                .collect(Collectors.toList());

        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);

        // ── Post events ──────────────────────────────────────
        List<Post> posts = postRepository.findRecentByUserIds(
                followingIds, since, PageRequest.of(0, 200));

        List<ActivityFeedItem> items = new ArrayList<>();

        for (Post p : posts) {
            if (p.getUser().isPrivateAccount()) continue; // skip private posts
            items.add(ActivityFeedItem.forPost(
                    p.getUser().getId(),
                    displayName(p.getUser()),
                    picUrl(p.getUser()),
                    p.getId(),
                    p.getContent(),
                    p.getImageUrl(),
                    p.getLikes() != null ? p.getLikes().size() : 0,
                    p.getComments() != null ? p.getComments().size() : 0,
                    p.getCreatedAt() != null ? p.getCreatedAt().format(ISO) : null));
        }

        // ── New-friend events (followings who followed someone new) ──
        for (Follow myFollow : myFollows) {
            User friend = myFollow.getFollowing();
            List<Follow> friendFollows = followRepository.findByFollowerId(friend.getId());
            for (Follow ff : friendFollows) {
                if (ff.getCreatedAt() != null && ff.getCreatedAt().isAfter(since)
                        && !ff.getFollowing().getId().equals(me.getId())) {
                    items.add(ActivityFeedItem.forNewFriend(
                            friend.getId(),
                            displayName(friend),
                            picUrl(friend),
                            ff.getFollowing().getId(),
                            displayName(ff.getFollowing()),
                            picUrl(ff.getFollowing()),
                            ff.getCreatedAt().format(ISO)));
                }
            }
        }

        // Sort descending by timestamp, paginate
        items.sort(Comparator.comparing(
                i -> i.getTimestamp() != null ? i.getTimestamp() : "",
                Comparator.reverseOrder()));

        int from = page * size;
        if (from >= items.size()) return List.of();
        return items.subList(from, Math.min(from + size, items.size()));
    }

    private String displayName(User u) {
        UserInfo info = u.getUserInfo();
        if (info != null) {
            String name = ((info.getFirstName() != null ? info.getFirstName() : "") + " "
                    + (info.getLastName() != null ? info.getLastName() : "")).trim();
            if (!name.isBlank()) return name;
        }
        return u.getEmail().split("@")[0];
    }

    private String picUrl(User u) {
        return u.getUserInfo() != null ? u.getUserInfo().getProfilePicUrl() : null;
    }
}
