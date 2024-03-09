package com.dp.dplanner.repository;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ClubMemberDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    Optional<ClubMember> findByClubIdAndMemberId(Long clubId, Long memberId);

    @Query("select cm from ClubMember cm where cm.club = :club order by cm.isConfirmed desc, cm.name")
    List<ClubMember> findAllByClub(Club club);


    @Query(value = "select cm from ClubMember cm where cm.club = :club and cm.isConfirmed = true")
    List<ClubMember> findAllConfirmedClubMemberByClub(@Param("club") Club club);

    @Query(value = "select cm from ClubMember cm where cm.club = :club and cm.isConfirmed = false")
    List<ClubMember> findAllUnconfirmedClubMemberByClub(@Param("club") Club club);

    @Query("SELECT cm FROM ClubMember cm " +
            "LEFT JOIN ClubAuthority ca ON cm.clubAuthority.id = ca.id " +
            "WHERE cm.club.id = :clubId AND :authorityType MEMBER OF ca.clubAuthorityTypes")
    List<ClubMember> findClubMemberByClubIdAndClubAuthorityTypesContaining(
            @Param("clubId") Long clubId,
            @Param("authorityType") ClubAuthorityType authorityType
    );

    @Query(value = """
    SELECT
        cm.id, cm.club_id as clubId, cm.name, cm.info, cm.role, cm.is_confirmed as isConfirmed, cm.url,
        ca2.id AS clubAuthorityId, ca2.name clubAuthorityName, ca2.clubAuthorityTypes
    FROM
        club_member cm
    LEFT JOIN
        (
            SELECT
                ca.*,
                ARRAY_AGG(ct.club_authority_types) AS clubAuthorityTypes
            FROM
                club_authority ca
            JOIN
                club_authority_club_authority_types AS ct ON ca.id = ct.club_authority_id
            GROUP BY
                ca.id
        ) AS ca2 ON cm.club_authority_id = ca2.id
        WHERE cm.id = :clubMemberId
""", nativeQuery = true)

    Optional<ClubMemberDto.ResponseMapping> findClubMemberWithClubAuthority(@Param("clubMemberId") Long clubMemberId);

    @Modifying
    @Query("""
    update ClubMember cm set cm.clubAuthority = null where cm.clubAuthority = :authority
""")
    void deleteClubAuthority(@Param("authority") ClubAuthority authority);
}
