package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c,  l.id, (select count(*) from CommentMemberLike l2 where l2.comment.id = c.id ) as likeCount " +
            "FROM Comment c " +
            "LEFT JOIN FETCH c.parent " +
            "JOIN FETCH c.clubMember " +
            "LEFT JOIN CommentMemberLike l on c.id = l.comment.id and l.clubMember.id = :clubMemberId " +
            "WHERE c.clubMember.id = :clubMemberId " +
            "ORDER BY c.parent.id ASC NULLS FIRST, c.createdDate ASC")
    List<Object[]> findCommentsByClubMemberId(@Param(value = "clubMemberId") Long clubMemberId);

    @Query("SELECT c, l.id, (select count(*) from CommentMemberLike l2 where l2.comment.id = c.id ) as likeCount " +
            "FROM Comment c " +
            "LEFT JOIN FETCH c.parent " +
            "JOIN FETCH c.clubMember " +
            "LEFT JOIN CommentMemberLike l on c.id = l.comment.id and l.clubMember.id = :clubMemberId " +
            "WHERE c.post.id = :postId " +
            "ORDER BY c.parent.id ASC NULLS FIRST, c.createdDate ASC")
    List<Object[]> findCommentsUsingPostId(@Param(value = "postId") Long postId,@Param(value = "clubMemberId") Long club_member_id);

    Long countDistinctByPostId(Long postId);
}
