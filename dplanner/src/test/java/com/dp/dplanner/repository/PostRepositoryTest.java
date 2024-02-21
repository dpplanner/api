package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
public class PostRepositoryTest {

    @Autowired
    PostRepository postRepository;

    @Autowired
    TestEntityManager testEntityManager;

    Club club;
    Member member;
    ClubMember clubMember;

    PageRequest pageRequest;

    @BeforeEach
    public void setUp() {
        club = Club.builder()
                .clubName("test")
                .build();

        member = Member.builder()
                .build();

        clubMember = ClubMember.builder()
                .member(member)
                .club(club)
                .name("testName")
                .info("test")
                .build();

        testEntityManager.persist(club);
        testEntityManager.persist(member);
        testEntityManager.persist(clubMember);
    }


    private Post createPost(Club club, ClubMember clubMember) {
        return Post.builder()
                .club(club)
                .clubMember(clubMember)
                .isFixed(false)
                .build();
    }

    @Test
    public void PostRepository_Save_ReturnSavedPost() {

        Post post = createPost(club,clubMember);

        Post savedPost = postRepository.save(post);

        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getId()).isGreaterThan(0);
        assertThat(savedPost.getClubMember()).isEqualTo(clubMember);
        assertThat(savedPost.getClubMember().getName()).isEqualTo("testName");
        assertThat(savedPost.getCreatedDate()).isNotNull();
        assertThat(savedPost.getLastModifiedDate()).isNotNull();
    }

    @Test
    public void PostRepository_GetAll_ReturnMoreThanOnePost() {

        Post post = createPost(club,clubMember);
        Post post2 = createPost(club,clubMember);
        Post post3 = createPost(null,clubMember);

        postRepository.save(post);
        postRepository.save(post2);
        postRepository.save(post3);

        List<Post> postList = postRepository.findAll();

        assertThat(postList).isNotNull();
        assertThat(postList).extracting(Post::getId).isNotNull();
        assertThat(postList.size()).isEqualTo(3);
        assertThat(postList).containsExactly(post, post2, post3);
    }

    @Test
    public void PostRepository_GetAllByClubId_ReturnMoreThanOnePost() throws InterruptedException {


        Post post = createPost(club,clubMember);
        Post post2 = createPost(club,clubMember);
        Post post3 = createPost(null,clubMember);

        postRepository.save(post);
        Thread.sleep(100);
        postRepository.save(post2);
        postRepository.save(post3);

        pageRequest = PageRequest.of(0, 2);
        Slice<Object[]> postSlice = postRepository.findByClubId(club.getId(),clubMember.getId(),pageRequest);
        List<Post> posts = postSlice.stream().map(object -> (Post) object[0]).toList();

        assertThat(posts).isNotNull();
        assertThat(posts).extracting(Post::getId).isNotNull();
        assertThat(posts.size()).isEqualTo(2);
        assertThat(posts).containsExactly(post2, post);

    }

    @Test
    @DisplayName("post order by isFixed desc, createDate desc")
    public void PostRepository_GetAllByClubIdOrderBy_ReturnMoreThanOnePost() throws Exception {
        Post post1 = createPost(club, clubMember);
        Post post2 = createPost(club, clubMember);
        Post post3 = createPost(club, clubMember);
        Post post4 = createPost(club, clubMember);

        post1.toggleIsFixed();
        post2.toggleIsFixed();

        postRepository.save(post1);
        Thread.sleep(100);
        postRepository.save(post2);
        Thread.sleep(100);
        postRepository.save(post3);
        Thread.sleep(100);
        postRepository.save(post4);

        Slice<Object[]> postList = postRepository.findByClubId(club.getId(),clubMember.getId(), PageRequest.of(0, 2));
        Slice<Object[]> postList2 = postRepository.findByClubId(club.getId(),clubMember.getId(),PageRequest.of(1, 2));
        Slice<Object[]> postList3 = postRepository.findByClubId(club.getId(),clubMember.getId(),PageRequest.of(0, 10));


        assertThat(postList.stream().map(object -> (Post) object[0]).collect(Collectors.toList())).containsExactly(post2, post1);
        assertThat(postList.hasNext()).isTrue();

        assertThat(postList2.stream().map(object -> (Post) object[0]).collect(Collectors.toList())).containsExactly(post4, post3);
        assertThat(postList2.hasNext()).isFalse();

        assertThat(postList3.stream().map(object -> (Post) object[0]).collect(Collectors.toList())).containsExactly(post2, post1, post4, post3);
        assertThat(postList3.hasNext()).isFalse();
    }
    @Test
    public void PostRepository_FindById_ReturnPost() {

        Post post = createPost(club,clubMember);

        postRepository.save(post);

        Post findPost = postRepository.findById(post.getId()).get();

        assertThat(findPost).isNotNull();
        assertThat(findPost).isEqualTo(post);
        assertThat(findPost.getId()).isGreaterThan(0);
    }

    @Test
    public void PostRepository_PostDelete_ReturnPostEmpty() {

        Post post = createPost(club,clubMember);

        postRepository.save(post);

        postRepository.deleteById(post.getId());

        assertThat(postRepository.findById(post.getId())).isEmpty();
    }

    @Test
    public void PostRepository_findMyPostsByClubId_ReturnPosts() {
        Post post1 = createPost(club, clubMember);
        Post post2 = createPost(club, clubMember);
        Post post3 = createPost(club, clubMember);
        Post post4 = createPost(club, clubMember);

        Club newClub = Club.builder().build();
        ClubMember newClubMember = ClubMember.createClubMember(member, newClub);
        testEntityManager.persist(newClub);
        testEntityManager.persist(newClubMember);
        Post post5 = createPost(newClub, newClubMember);


        postRepository.save(post1);
        postRepository.save(post2);
        postRepository.save(post3);
        postRepository.save(post4);
        postRepository.save(post5);


        Slice<Object[]> postSlice = postRepository.findMyPostsByClubId(clubMember.getId(), club.getId(), Pageable.unpaged());

        assertThat(postSlice).isNotNull();
        assertThat(postSlice.getContent().size()).isEqualTo(4);
        assertThat(postSlice.stream().map(object -> (Post) object[0]).collect(Collectors.toList()))
                .extracting(Post::getClubMember).extracting(ClubMember::getId).containsOnly(clubMember.getId());




    }


}
