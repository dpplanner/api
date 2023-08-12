package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p " +
            "from Post p " +
            "where p.club.id = :clubId " +
            "order by p.isFixed desc, p.createdDate desc")
    Slice<Post> findByClubId(@Param(value = "clubId") Long clubId, Pageable pageable);
}
