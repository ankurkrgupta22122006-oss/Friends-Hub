package com.example.socialmedia.repository;

import com.example.socialmedia.entity.ChatGroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGroupMessageRepository extends JpaRepository<ChatGroupMessage, Long> {
    List<ChatGroupMessage> findByGroupIdOrderByCreatedAtAsc(Long groupId);

    Optional<ChatGroupMessage> findTopByGroupIdOrderByCreatedAtDesc(Long groupId);

    @Query("SELECT m FROM ChatGroupMessage m WHERE m.id IN (" +
           "SELECT MAX(m2.id) FROM ChatGroupMessage m2 WHERE m2.group.id IN :groupIds GROUP BY m2.group.id)")
    List<ChatGroupMessage> findLatestMessagesByGroupIds(@Param("groupIds") List<Long> groupIds);
}
