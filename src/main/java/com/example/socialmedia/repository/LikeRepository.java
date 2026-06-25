package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Like;
import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndPost(User user, Post post);

    boolean existsByUserIdAndPostId(Long userId, Long postId);
    boolean existsByUserEmailAndPostId(String email, Long postId);

    long countByPostId(Long postId);

    // Count likes user A gave on posts authored by user B
    @Query("SELECT COUNT(l) FROM Like l WHERE l.user = :liker AND l.post.user = :postOwner")
    long countLikesByLikerOnOwnerPosts(@Param("liker") User liker, @Param("postOwner") User postOwner);
}
