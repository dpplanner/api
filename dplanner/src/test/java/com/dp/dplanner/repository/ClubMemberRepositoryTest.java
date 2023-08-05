package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.club.ClubRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class ClubMemberRepositoryTest {

    @Autowired
    ClubMemberRepository clubMemberRepository;

    @Autowired
    TestEntityManager testEntityManager;

    Club club;
    Member member;

    @BeforeEach
    void setUp() {
        club = Club.builder().clubName("newClub").build();
        member = Member.builder().build();
        testEntityManager.persist(club);
        testEntityManager.persist(member);
    }

    @Test
    @DisplayName("ClubMember 저장")
    public void save() throws Exception {
        //given
        ClubMember clubMember = ClubMember.builder().member(member).club(club).build();
        //when
        ClubMember savedClubMember = clubMemberRepository.save(clubMember);
        //then
        assertThat(savedClubMember.getId()).isNotNull();
        assertThat(savedClubMember.getClub()).isEqualTo(club);
        assertThat(savedClubMember.getMember()).isEqualTo(member);
        assertThat(savedClubMember.getIsConfirmed()).as("관리자의 승인 전까지 isConfirmed는 false").isFalse();
        assertThat(savedClubMember.getRole()).as("clubMember는 기본적으로 USER 권한").isEqualTo(ClubRole.USER);
    }

    @Test
    @DisplayName("club과 member로 clubMember조회")
    @Disabled("아직 사용되지 않음")
    public void findByClubAndMember() throws Exception {
        //given
        ClubMember clubMember = ClubMember.builder().member(member).club(club).build();
        ClubMember savedClubMember = testEntityManager.persist(clubMember);

        //when
        ClubMember findClubMember = clubMemberRepository.findByClubAndMember(club, member).orElse(null);

        //then
        assertThat(findClubMember.getId()).isEqualTo(savedClubMember.getId());
        assertThat(findClubMember.getClub()).isEqualTo(club);
        assertThat(findClubMember.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("clubId와 memberId로 clubMember조회")
    public void findByClubIdAndMemberId() throws Exception {
        //given
        ClubMember clubMember = ClubMember.builder().member(member).club(club).build();
        ClubMember savedClubMember = testEntityManager.persist(clubMember);

        //when
        ClubMember findClubMember = clubMemberRepository.findByClubIdAndMemberId(club.getId(), member.getId()).orElse(null);

        //then
        assertThat(findClubMember.getId()).isEqualTo(savedClubMember.getId());
        assertThat(findClubMember.getClub()).isEqualTo(club);
        assertThat(findClubMember.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("club으로 clubMember 리스트 조회")
    public void findAllByClub() throws Exception {
        //given
        Member member2 = Member.builder().build();
        testEntityManager.persist(member2);

        ClubMember clubMember1 = ClubMember.builder().member(member).club(club).build();
        ClubMember clubMember2 = ClubMember.builder().member(member2).club(club).build();
        ClubMember savedClubMember1 = testEntityManager.persist(clubMember1);
        ClubMember savedClubMember2 = testEntityManager.persist(clubMember2);


        //when
        List<ClubMember> findClubMembers = clubMemberRepository.findAllByClub(club);

        //then
        assertThat(findClubMembers).containsExactly(savedClubMember1, savedClubMember2);
    }

    @Test
    @DisplayName("클럽의 승인된 회원만 조회")
    public void findAllConfirmedClubMemberByClub() throws Exception {
        //given
        Member unconfirmedMember = Member.builder().build();
        testEntityManager.persist(unconfirmedMember);

        ClubMember confirmedMember = ClubMember.builder().club(club).member(member).build();
        confirmedMember.confirm();
        testEntityManager.persist(confirmedMember);

        ClubMember unConfirmedMember = ClubMember.builder().club(club).member(unconfirmedMember).build();
        testEntityManager.persist(unConfirmedMember);

        //when
        List<ClubMember> confirmedMembers = clubMemberRepository.findAllConfirmedClubMemberByClub(club);

        //then
        assertThat(confirmedMembers).as("승인된 회원만 포함해야 함.").containsExactly(confirmedMember);
        assertThat(confirmedMembers).as("승인되지 않은 회원은 포함하지 않아야 함.").doesNotContain(unConfirmedMember);
    }

    @Test
    @DisplayName("클럽의 승인되지 않은 회원만 조회")
    public void findAllUnConfirmedClubMemberByClub() throws Exception {
        //given
        Member unconfirmedMember = Member.builder().build();
        testEntityManager.persist(unconfirmedMember);

        ClubMember confirmedMember = ClubMember.builder().club(club).member(member).build();
        confirmedMember.confirm();
        testEntityManager.persist(confirmedMember);

        ClubMember unConfirmedMember = ClubMember.builder().club(club).member(unconfirmedMember).build();
        testEntityManager.persist(unConfirmedMember);

        //when
        List<ClubMember> unConfirmedMembers = clubMemberRepository.findAllUnConfirmedClubMemberByClub(club);

        //then
        assertThat(unConfirmedMembers).as("승인되지 않은 회원만 포함해야 함.").containsExactly(unConfirmedMember);
        assertThat(unConfirmedMembers).as("승인된 회원은 포함하지 않아야 함.").doesNotContain(confirmedMember);
    }
}