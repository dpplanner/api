package com.dp.dplanner.repository;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.dto.ClubDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    @Query(value = """
    SELECT
        c.id AS id, c.club_name AS clubName, c.info, COUNT(cm.club_id) AS memberCount,
        (
            SELECT cm2.is_confirmed
            FROM club_member cm2
            WHERE cm2.member_id = :memberId
            AND cm2.club_id = c.id
        ) as isConfirmed
    FROM
        club c
    LEFT JOIN
        club_member cm ON c.id = cm.club_id
    WHERE
        EXISTS (
            SELECT 1
            FROM club_member cm2
            WHERE cm2.member_id = :memberId
            AND cm2.club_id = cm.club_id
        )
    GROUP BY
        c.id
    ORDER BY
        c.id
        """, nativeQuery = true)

    List<ClubDto.ResponseMapping> findMyClubs(@Param("memberId") Long memberId);


    @Query(value = """
        SELECT  c.id AS id, c.club_name as clubName, c.info, COUNT(cm.club_id) AS memberCount
        FROM club c
        LEFT JOIN club_member cm ON c.id = cm.club_id
        WHERE c.id = :clubId
        GROUP BY c.id
        """, nativeQuery = true)
    ClubDto.ResponseMapping findClubDtoByClubId(@Param("clubId") Long clubId);
}
