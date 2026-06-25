package com.example.socialmedia.repository;

import com.example.socialmedia.entity.FollowRequest;
import com.example.socialmedia.entity.FollowRequestStatus;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    List<FollowRequest> findByTargetIdAndStatus(Long targetId, FollowRequestStatus status);

    Optional<FollowRequest> findByRequesterAndTarget(User requester, User target);

    long countByRequesterId(Long requesterId);

    long countByRequesterIdAndStatus(Long requesterId, FollowRequestStatus status);

    long countByTargetId(Long targetId);

    long countByTargetIdAndStatus(Long targetId, FollowRequestStatus status);

    // Monthly sent counts for the last N months: [year, month, count]
    @Query("SELECT FUNCTION('YEAR', r.createdAt), FUNCTION('MONTH', r.createdAt), COUNT(r) " +
           "FROM FollowRequest r WHERE r.requester.id = :userId AND r.createdAt >= :since " +
           "GROUP BY FUNCTION('YEAR', r.createdAt), FUNCTION('MONTH', r.createdAt) " +
           "ORDER BY FUNCTION('YEAR', r.createdAt), FUNCTION('MONTH', r.createdAt)")
    List<Object[]> countSentByMonth(@Param("userId") Long userId, @Param("since") java.time.LocalDateTime since);

    // Monthly received counts for the last N months: [year, month, count]
    @Query("SELECT FUNCTION('YEAR', r.createdAt), FUNCTION('MONTH', r.createdAt), COUNT(r) " +
           "FROM FollowRequest r WHERE r.target.id = :userId AND r.createdAt >= :since " +
           "GROUP BY FUNCTION('YEAR', r.createdAt), FUNCTION('MONTH', r.createdAt) " +
           "ORDER BY FUNCTION('YEAR', r.createdAt), FUNCTION('MONTH', r.createdAt)")
    List<Object[]> countReceivedByMonth(@Param("userId") Long userId, @Param("since") java.time.LocalDateTime since);
}
