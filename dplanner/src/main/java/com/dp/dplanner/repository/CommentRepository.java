package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.parent " +
            "JOIN FETCH c.clubMember " +
            "WHERE c.clubMember.id = :clubMemberId " +
            "ORDER BY c.parent.id ASC NULLS FIRST, c.createdDate ASC")
    List<Comment> findCommentsByClubMemberId(@Param(value = "clubMemberId") Long clubMemberId);

    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.parent " +
            "JOIN FETCH c.clubMember " +
            "WHERE c.post.id = :postId " +
            "ORDER BY c.parent.id ASC NULLS FIRST, c.createdDate ASC")
    List<Comment> findCommentsUsingPostId(@Param(value = "postId") Long postId);

    int countDistinctByPostId(Long postId);
}
