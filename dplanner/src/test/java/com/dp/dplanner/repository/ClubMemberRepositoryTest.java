package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.club.ClubRole;
import org.junit.jupiter.api.BeforeEach;
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
    @DisplayName("clubMemberId로 조회")
    public void findById() throws Exception {
        //given
        ClubMember clubMember = ClubMember.builder().member(member).club(club).build();
        ClubMember savedClubMember = testEntityManager.persist(clubMember);

        //when
        ClubMember findClubMember = clubMemberRepository.findById(savedClubMember.getId()).orElse(null);

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
    @DisplayName("clubMemberId로 리스트 조회")
    public void findAllById() throws Exception {
        //given
        Member member2 = Member.builder().build();
        testEntityManager.persist(member2);

        ClubMember clubMember1 = ClubMember.builder().member(member).club(club).build();
        ClubMember clubMember2 = ClubMember.builder().member(member2).club(club).build();
        ClubMember savedClubMember1 = testEntityManager.persist(clubMember1);
        ClubMember savedClubMember2 = testEntityManager.persist(clubMember2);


        //when
        List<ClubMember> findClubMembers =
                clubMemberRepository.findAllById(List.of(savedClubMember1.getId(), savedClubMember2.getId()));

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
        List<ClubMember> unConfirmedMembers = clubMemberRepository.findAllUnconfirmedClubMemberByClub(club);

        //then
        assertThat(unConfirmedMembers).as("승인되지 않은 회원만 포함해야 함.").containsExactly(unConfirmedMember);
        assertThat(unConfirmedMembers).as("승인된 회원은 포함하지 않아야 함.").doesNotContain(confirmedMember);
    }

    @Test
    @DisplayName("클럽회원 삭제")
    public void delete() throws Exception {
        //given
        ClubMember clubMember = ClubMember.builder().member(member).club(club).build();
        ClubMember savedClubMember = testEntityManager.persist(clubMember);

        //when
        clubMemberRepository.delete(savedClubMember);

        //then
        ClubMember findClubMember = testEntityManager.find(ClubMember.class, savedClubMember.getId());
        assertThat(findClubMember).as("영속성 컨텍스트에 삭제된 회원이 없어야 함").isNull();
    }

    @Test
    @DisplayName("클럽회원 여러명 삭제")
    public void deleteAll() throws Exception {
        //given
        Member member2 = Member.builder().build();
        testEntityManager.persist(member2);

        ClubMember clubMember1 = ClubMember.builder().member(member).club(club).build();
        ClubMember clubMember2 = ClubMember.builder().member(member2).club(club).build();
        ClubMember savedClubMember1 = testEntityManager.persist(clubMember1);
        ClubMember savedClubMember2 = testEntityManager.persist(clubMember2);


        //when
        clubMemberRepository.deleteAll(List.of(savedClubMember1, savedClubMember2));

        //then
        ClubMember findClubMember1 = testEntityManager.find(ClubMember.class, savedClubMember1.getId());
        ClubMember findClubMember2 = testEntityManager.find(ClubMember.class, savedClubMember2.getId());
        assertThat(findClubMember1).as("영속성 컨텍스트에 삭제된 회원이 없어야 함").isNull();
        assertThat(findClubMember2).as("영속성 컨텍스트에 삭제된 회원이 없어야 함").isNull();
    }
}