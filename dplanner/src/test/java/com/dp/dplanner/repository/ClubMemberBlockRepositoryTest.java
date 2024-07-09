package com.dp.dplanner.repository;

import com.dp.dplanner.domain.ClubMemberBlock;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
public class ClubMemberBlockRepositoryTest {


    @Autowired
    ClubMemberBlockRepository clubMemberBlockRepository;

    @Autowired
    TestEntityManager testEntityManager;

    Club club;
    Member member;
    ClubMember clubMember;

    Member blockedMember;
    ClubMember blockedClubMember;


    @BeforeEach
    public void setUp() {
        club = Club.builder().build();
        member = Member.builder().build();
        clubMember = ClubMember.createClubMember(member, club);

        blockedMember = Member.builder().build();
        blockedClubMember = ClubMember.createClubMember(blockedMember, club);

        testEntityManager.persist(club);
        testEntityManager.persist(member);
        testEntityManager.persist(clubMember);

        testEntityManager.persist(blockedMember);
        testEntityManager.persist(blockedClubMember);
    }

    @Test
    public void ResourceRepository_createResource_ReturnResource() {


        ClubMemberBlock block = clubMember.block(blockedClubMember);
        ClubMemberBlock blockedClubMember = clubMemberBlockRepository.save(block);

        assertThat(block.getId()).isEqualTo(blockedClubMember.getId());

    }

}
