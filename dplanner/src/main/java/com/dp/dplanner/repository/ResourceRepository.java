package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource,Long> {

    @Query("""
            SELECT r
            FROM Resource r
            WHERE r.club.id = :clubId
            ORDER BY r.name
            """)
    List<Resource> findByClubId(@Param(value = "clubId") Long clubId);

}
