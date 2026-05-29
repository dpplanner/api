package com.dp.dplanner.repository;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.adapter.dto.ClubDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    @Query(value = """
        SELECT
            c.id AS id, c.club_name AS clubName, c.info, c.url,
            (SELECT count(*)
             FROM club_member cm2
             WHERE cm2.club_id = c.id AND cm2.is_confirmed = 1 AND cm2.is_deleted = 0) memberCount,
            cm.is_confirmed as isConfirmed
        FROM
            club c
        LEFT JOIN
            club_member cm ON c.id = cm.club_id
        WHERE
            cm.member_id = :memberId and cm.is_deleted = 0
        ORDER BY
            c.id
        """, nativeQuery = true)
    List<ClubDto.ResponseMapping> findMyClubs(@Param("memberId") Long memberId);


    @Query(value = """
        SELECT  c.id AS id, c.club_name as clubName, c.info, c.url,
            (SELECT count(*)
             FROM club_member cm2
             WHERE cm2.club_id = c.id AND cm2.is_confirmed = 1 AND cm2.is_deleted = 0) memberCount
        FROM club c
        WHERE c.id = :clubId
        """, nativeQuery = true)
    ClubDto.ResponseMapping findClubDtoByClubId(@Param("clubId") Long clubId);
}
