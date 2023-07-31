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
        member = Member.builder().name("user1").build();
        testEntityManager.persist(club);
        testEntityManager.persist(member);
    }

    @Test
    @DisplayName("ClubMember가 정상적으로 저장됨")
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
    public void findByMember() throws Exception {
        //given
        ClubMember clubMember = ClubMember.builder().member(member).club(club).build();
        ClubMember savedClubMember = clubMemberRepository.save(clubMember);

        //when
        ClubMember findClubMember = clubMemberRepository.findByClubAndMember(club, member).orElse(null);

        //then
        assertThat(findClubMember.getId()).isEqualTo(savedClubMember.getId());
        assertThat(findClubMember.getClub()).isEqualTo(club);
        assertThat(findClubMember.getMember()).isEqualTo(member);
    }
}