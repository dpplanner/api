package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    Optional<ClubMember> findByClubAndMember(Club club, Member member);

    Optional<ClubMember> findByClubIdAndMemberId(Long clubId, Long memberId);

    List<ClubMember> findAllByClub(Club club);


    @Query(value = "select cm from ClubMember cm where cm.club = :club and cm.isConfirmed = true")
    List<ClubMember> findAllConfirmedClubMemberByClub(@Param("club") Club club);

    @Query(value = "select cm from ClubMember cm where cm.club = :club and cm.isConfirmed = false")
    List<ClubMember> findAllUnconfirmedClubMemberByClub(@Param("club") Club club);

}
