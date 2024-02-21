package com.dp.dplanner.repository;

import com.dp.dplanner.domain.CommentMemberLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentMemberLikeRepository extends JpaRepository<CommentMemberLike,Long> {
    Optional<CommentMemberLike> findCommentMemberLikeByClubMemberIdAndCommentId(Long clubMemberId, Long commentId);

    Long countDistinctByCommentId(Long CommentId);
}
