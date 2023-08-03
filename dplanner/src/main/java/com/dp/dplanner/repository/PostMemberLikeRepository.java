package com.dp.dplanner.repository;

import com.dp.dplanner.domain.PostMemberLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostMemberLikeRepository extends JpaRepository<PostMemberLike, Long> {

    Optional<PostMemberLike> findPostMemberLikeByMemberIdAndPostId(long memberId, long postId);

}
