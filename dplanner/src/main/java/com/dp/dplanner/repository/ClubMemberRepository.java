package com.dp.dplanner.repository;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.adapter.dto.ClubMemberDto;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMemberRepository extends CrudRepository<ClubMember, Long> {

    @Query("select cm from ClubMember cm join fetch cm.member where cm.id =:clubMemberId and cm.isDeleted=false ")
    Optional<ClubMember> findById(@Param("clubMemberId") Long clubMemberId);

    @Query("select cm from ClubMember cm join fetch cm.member where cm.id in :clubMemberIds and cm.isDeleted=false ")
    List<ClubMember> findAllById(@Param("clubMemberIds") List<Long> clubMemberIds);

    @Query("SELECT cm from ClubMember  cm where cm.club.id = :clubId and cm.member.id = :memberId and cm.isDeleted=false ")
    Optional<ClubMember> findByClubIdAndMemberId(Long clubId, Long memberId);

    @Query("select cm from ClubMember cm where cm.club = :club and cm.isDeleted=false order by cm.isConfirmed desc, cm.name")
    List<ClubMember> findAllByClub(Club club);


    @Query(value = "select cm from ClubMember cm " +
            " left join ClubAuthority ca on cm.clubAuthority.id = ca.id " +
            " where cm.club = :club and cm.isConfirmed = true and cm.isDeleted=false " +
            " order by cm.role, ca.name, cm.name ")
    List<ClubMember> findAllConfirmedClubMemberByClub(@Param("club") Club club);

    @Query(value = "select cm from ClubMember cm where cm.club = :club and cm.isConfirmed = false and cm.isDeleted=false  order by cm.name")
    List<ClubMember> findAllUnconfirmedClubMemberByClub(@Param("club") Club club);

    @Query("SELECT cm FROM ClubMember cm " +
            "LEFT JOIN ClubAuthority ca ON cm.clubAuthority.id = ca.id " +
            "WHERE cm.club.id = :clubId AND ((:authorityType MEMBER OF ca.clubAuthorityTypes) or cm.role = 'ADMIN') AND cm.isDeleted=false")
    List<ClubMember> findClubMemberByClubIdAndClubAuthorityTypesContaining(
            @Param("clubId") Long clubId,
            @Param("authorityType") ClubAuthorityType authorityType
    );

    @Query(value = """
        SELECT
            cm.id, cm.club_id AS clubId, cm.name, cm.info, cm.role, cm.is_confirmed AS isConfirmed, cm.url,
            ca2.clubAuthorityId, ca2.clubAuthorityName, ca2.clubAuthorityTypes
        FROM
            club_member cm
        LEFT JOIN
            (
                SELECT
                    ca.id AS clubAuthorityId,
                    ca.name AS clubAuthorityName,
                    LISTAGG(ct.club_authority_types, ',') WITHIN GROUP (ORDER BY ct.club_authority_types) AS clubAuthorityTypes
                FROM
                    club_authority ca
                JOIN
                    club_authority_club_authority_types ct ON ca.id = ct.club_authority_id
                GROUP BY
                    ca.id, ca.name
            ) ca2 ON cm.club_authority_id = ca2.clubAuthorityId
        WHERE
            cm.id = :clubMemberId AND cm.is_deleted = 0
    """, nativeQuery = true)
    Optional<ClubMemberDto.ResponseMapping> findClubMemberWithClubAuthority(@Param("clubMemberId") Long clubMemberId);

    @Modifying
    @Query("""
    update ClubMember cm set cm.clubAuthority = null , cm.role = 'USER' where cm.clubAuthority = :authority
    """)
    void deleteClubAuthority(@Param("authority") ClubAuthority authority);
}
