package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Comment;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase
public class CommentRepositoryTest {


    @Autowired
    CommentRepository commentRepository;
    @Autowired
    TestEntityManager testEntityManager;


    Club club;
    Member member;
    ClubMember clubMember;
    Post post;

    private Comment createComment(ClubMember clubMember, Post post, Comment parent) {

        Comment comment = Comment.builder()
                .clubMember(clubMember)
                .post(post)
                .parent(parent)
                .build();
        return comment;
    }

    @BeforeEach
    public void setUp() {
        member = Member.builder().build();
        testEntityManager.persist(member);

        club = Club.builder().clubName("test").info("test").build();
        testEntityManager.persist(club);

        clubMember = ClubMember.builder()
                .member(member)
                .club(club)
                .name("test")
                .info("test")
                .build();

        testEntityManager.persist(clubMember);

        post = Post.builder()
                .club(club)
                .clubMember(clubMember)
                .content("test")
                .isFixed(false)
                .build();
        testEntityManager.persist(post);

    }

    @Test
    public void CommentRepository_SaveComment_ReturnSavedComment(){

        Comment savedComment = commentRepository.save(createComment(clubMember,post ,null));

        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getId()).isGreaterThan(0L);
        assertThat(savedComment.getParent()).isNull();

        assertThat(post.getComments().size()).isEqualTo(1);
        assertThat(post.getComments().get(0)).isEqualTo(savedComment);

    }

    @Test
    public void CommentRepository_SaveReplyComment_ReturnSavedComment(){

        Comment parentComment = commentRepository.save(createComment(clubMember,post ,null));

        Comment savedReplyComment = commentRepository.save(createComment(clubMember,post ,parentComment));
        Comment savedReplyComment2 = commentRepository.save(createComment(clubMember, post, parentComment));


        assertThat(savedReplyComment).isNotNull();
        assertThat(savedReplyComment.getId()).isGreaterThan(0L);
        assertThat(savedReplyComment.getParent().getId()).isEqualTo(parentComment.getId());
        assertThat(parentComment.getChildren().size()).isEqualTo(2);
        assertThat(post.getComments().size()).isEqualTo(3);
    }

    @Test
    public void CommentRepository_findById_ReturnComment(){

        Comment comment = commentRepository.save(createComment(clubMember,post ,null));

        Comment foundComment = commentRepository.findById(comment.getId()).get();

        assertThat(foundComment).isNotNull();
        assertThat(foundComment.getId()).isEqualTo(comment.getId());
    }

    @Test
    public void CommentRepository_findByClubMemberId_ReturnComment() {

        Comment comment = commentRepository.save(createComment(clubMember,post ,null));

        List<Comment> foundComments = commentRepository.findCommentsByClubMemberId(clubMember.getId());

        assertThat(foundComments).isNotNull();
        assertThat(foundComments.size()).isEqualTo(1);
        assertThat(foundComments)
                .extracting(Comment::getClubMember)
                .extracting(ClubMember::getId)
                .containsOnly(clubMember.getId());

    }

    @Test
    public void CommentRepository_findByPostId_ReturnComments() {

        Comment comment = commentRepository.save(createComment(clubMember,post ,null));
        Comment comment2 = commentRepository.save(createComment(clubMember,post ,null));
        Comment comment3 = commentRepository.save(createComment(clubMember,post ,comment));
        Comment comment4 = commentRepository.save(createComment(clubMember,post ,comment));
        Comment comment5 = commentRepository.save(createComment(clubMember,post ,comment2));

        List<Comment> commentsList = commentRepository.findCommentsUsingPostId(post.getId());

        assertThat(commentsList.size()).isEqualTo(5);
        assertThat(commentsList).extracting(Comment::getPost).extracting(Post::getId).containsOnly(post.getId());
    }

    @Test
    public void CommentRepository_delete_ReturnVoid() {

        Comment comment = commentRepository.save(createComment(clubMember, post, null));
        Comment comment2 = commentRepository.save(createComment(clubMember,post ,comment));

        commentRepository.delete(comment);

        assertThat(commentRepository.findById(comment.getId())).isEmpty();
        assertThat(commentRepository.findById(comment2.getId())).isEmpty();

    }

    @Test
    public void CommentRepository_countDistinctByPostId_ReturnInt() {


        Comment comment = commentRepository.save(createComment(clubMember, post, null));
        Comment comment2 = commentRepository.save(createComment(clubMember, post, comment));

        Post post2 = Post.builder().build();
        testEntityManager.persist(post2);
        Comment comment3 = commentRepository.save(createComment(clubMember, post2, null));

        int commentCount = commentRepository.countDistinctByPostId(post.getId());
        assertThat(commentCount).isEqualTo(2);

    }
}
