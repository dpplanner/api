package com.dp.dplanner.repository;

import com.dp.dplanner.domain.*;
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
public class PostBlockRepositoryTest {


    @Autowired
    PostBlockRepository postBlockRepository;

    @Autowired
    TestEntityManager testEntityManager;

    Club club;
    Member member;
    ClubMember clubMember;
    Post post;

    @BeforeEach
    public void setUp() {
        club = Club.builder().build();
        member = Member.builder().build();
        clubMember = ClubMember.createClubMember(member, club);
        post = Post.builder().club(club).clubMember(clubMember).build();
        testEntityManager.persist(club);
        testEntityManager.persist(member);
        testEntityManager.persist(clubMember);
        testEntityManager.persist(post);
    }

    @Test
    public void ResourceRepository_createResource_ReturnResource() {

        testEntityManager.persist(post);

        PostBlock block = clubMember.block(post);
        PostBlock blockPost = postBlockRepository.save(block);

        assertThat(blockPost.getId()).isEqualTo(block.getId());

    }

}
