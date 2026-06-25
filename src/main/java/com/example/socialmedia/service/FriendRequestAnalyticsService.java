package com.example.socialmedia.service;

import com.example.socialmedia.dto.FriendRequestAnalyticsResponse;
import com.example.socialmedia.dto.FriendRequestAnalyticsResponse.MonthlyPoint;
import com.example.socialmedia.entity.FollowRequestStatus;
import com.example.socialmedia.repository.FollowRequestRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FriendRequestAnalyticsService {

    private static final int MONTHS_BACK = 6;

    private final UserRepository userRepository;
    private final FollowRequestRepository followRequestRepository;

    public FriendRequestAnalyticsService(UserRepository userRepository,
                                         FollowRequestRepository followRequestRepository) {
        this.userRepository = userRepository;
        this.followRequestRepository = followRequestRepository;
    }

    @Transactional(readOnly = true)
    public FriendRequestAnalyticsResponse getAnalytics(String email) {
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .getId();

        long totalSent     = followRequestRepository.countByRequesterId(userId);
        long pendingSent   = followRequestRepository.countByRequesterIdAndStatus(userId, FollowRequestStatus.PENDING);
        long acceptedSent  = followRequestRepository.countByRequesterIdAndStatus(userId, FollowRequestStatus.ACCEPTED);

        long totalReceived    = followRequestRepository.countByTargetId(userId);
        long pendingReceived  = followRequestRepository.countByTargetIdAndStatus(userId, FollowRequestStatus.PENDING);
        long acceptedReceived = followRequestRepository.countByTargetIdAndStatus(userId, FollowRequestStatus.ACCEPTED);

        LocalDateTime since = LocalDateTime.now().minusMonths(MONTHS_BACK).withDayOfMonth(1).toLocalDate().atStartOfDay();

        List<MonthlyPoint> monthlySent     = buildSeries(followRequestRepository.countSentByMonth(userId, since));
        List<MonthlyPoint> monthlyReceived = buildSeries(followRequestRepository.countReceivedByMonth(userId, since));

        return new FriendRequestAnalyticsResponse(
                totalSent, pendingSent, acceptedSent,
                totalReceived, pendingReceived, acceptedReceived,
                monthlySent, monthlyReceived);
    }

    private List<MonthlyPoint> buildSeries(List<Object[]> raw) {
        // raw rows: [year, month(1-12), count]
        Map<String, Long> dataMap = raw.stream().collect(Collectors.toMap(
                r -> r[0] + "-" + r[1],
                r -> ((Number) r[2]).longValue()
        ));

        List<MonthlyPoint> series = new ArrayList<>();
        LocalDateTime cursor = LocalDateTime.now().minusMonths(MONTHS_BACK - 1).withDayOfMonth(1);
        for (int i = 0; i < MONTHS_BACK; i++) {
            String key = cursor.getYear() + "-" + cursor.getMonthValue();
            String label = cursor.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            series.add(new MonthlyPoint(label, dataMap.getOrDefault(key, 0L)));
            cursor = cursor.plusMonths(1);
        }
        return series;
    }
}
