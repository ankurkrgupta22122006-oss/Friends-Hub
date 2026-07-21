package com.example.socialmedia.repository;

import com.example.socialmedia.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {
    List<ChatGroup> findByMembers_Id(Long userId);

    @Query("SELECT DISTINCT g FROM ChatGroup g LEFT JOIN FETCH g.createdBy cb LEFT JOIN FETCH g.members m LEFT JOIN FETCH m.userInfo WHERE :userId IN (SELECT m2.id FROM g.members m2)")
    List<ChatGroup> findGroupsWithMembersByUserId(@Param("userId") Long userId);

    Optional<ChatGroup> findByIdAndMembers_Id(Long groupId, Long userId);
}
