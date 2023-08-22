package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p " +
            "from Post p " +
            "join fetch p.clubMember " +
            "where p.club.id = :clubId " +
            "order by p.isFixed desc, p.createdDate desc")
    Slice<Post> findByClubId(@Param(value = "clubId") Long clubId, Pageable pageable);

    @Query("select p " +
            "from Post p " +
            "join fetch p.clubMember " +
            "where p.club.id = :clubId " +
            "and p.clubMember.id = :clubMemberId "  +
            "order by p.isFixed desc, p.createdDate desc")
    Slice<Post> findMyPostsByClubId(@Param(value = "clubMemberId") Long clubMemberId,@Param(value = "clubId") Long clubId, Pageable pageable);

    @Query("select p " +
            "from Post p " +
            "join fetch p.clubMember " +
            "where p.id = :id")
    Optional<Post> findById(@Param(value = "id")Long id);
}
