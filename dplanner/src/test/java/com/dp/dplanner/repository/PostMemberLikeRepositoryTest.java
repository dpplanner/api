package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class PostMemberLikeRepositoryTest {

    @Autowired
    PostMemberLikeRepository postMemberLikeRepository;

    @Autowired
    TestEntityManager testEntityManager;

    Post post;
    Club club;
    @BeforeEach
    public void setUp() {
        post = Post.builder().build();
        club = Club.builder().build();

        testEntityManager.persist(post);
        testEntityManager.persist(club);
    }
    @Test
    public void PostMemberLikeRepository_findByPostId() {

        Member member = Member.builder().build();
        ClubMember clubMember = ClubMember.builder().club(club).member(member).build();

        Member member2 = Member.builder().build();
        ClubMember clubMember2 = ClubMember.builder().club(club).member(member2).build();

        PostMemberLike like1 = PostMemberLike.builder().post(post).clubMember(clubMember).build();
        PostMemberLike like2 = PostMemberLike.builder().post(post).clubMember(clubMember2).build();

        testEntityManager.persist(member);
        testEntityManager.persist(clubMember);
        testEntityManager.persist(member2);
        testEntityManager.persist(clubMember2);

        postMemberLikeRepository.saveAll(Arrays.asList(like1, like2));

        int likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());

        assertThat(likeCount).isEqualTo(2);
    }
}
