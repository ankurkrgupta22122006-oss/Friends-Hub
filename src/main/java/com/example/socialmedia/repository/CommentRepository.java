package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPostId(Long postId, Pageable pageable);

    // Count comments user A left on posts authored by user B
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :commenterId AND c.post.user.id = :postOwnerId")
    long countCommentsByCommenterOnOwnerPosts(@Param("commenterId") Long commenterId,
                                              @Param("postOwnerId") Long postOwnerId);
}
