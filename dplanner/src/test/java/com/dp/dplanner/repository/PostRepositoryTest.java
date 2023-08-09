package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class PostRepositoryTest {

    @Autowired
    PostRepository postRepository;

    @Autowired
    TestEntityManager testEntityManager;

    private Club club;
    private Member member;
    private ClubMember clubMember;

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
                .name("test")
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
                .build();
    }

    @Test
    public void PostRepository_Save_ReturnSavedPost() {

        Post post = createPost(club,clubMember);

        Post savedPost = postRepository.save(post);

        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getId()).isGreaterThan(0);
        assertThat(savedPost.getClubMember()).isEqualTo(clubMember);
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
    public void PostRepository_GetAllByClubId_ReturnMoreThanOnePost() {


        Post post = createPost(club,clubMember);
        Post post2 = createPost(club,clubMember);
        Post post3 = createPost(null,clubMember);

        postRepository.save(post);
        postRepository.save(post2);
        postRepository.save(post3);

        List<Post> postList = postRepository.findByClubId(club.getId());

        assertThat(postList).isNotNull();
        assertThat(postList).extracting(Post::getId).isNotNull();
        assertThat(postList.size()).isEqualTo(2);
        assertThat(postList).containsExactly(post, post2);


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


}
