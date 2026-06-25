package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAll(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Post p WHERE p.user.isPrivateAccount = false")
    Page<Post> findAllPublicPosts(Pageable pageable);

    Page<Post> findByUserId(Long userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Post p WHERE p.user.id IN :userIds ORDER BY p.createdAt DESC")
    Page<Post> findByUserIdIn(java.util.List<Long> userIds, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        "SELECT p FROM Post p WHERE p.user.id IN :userIds AND p.createdAt >= :since ORDER BY p.createdAt DESC")
    java.util.List<Post> findRecentByUserIds(
        @org.springframework.data.repository.query.Param("userIds") java.util.List<Long> userIds,
        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,
        Pageable pageable);
}
