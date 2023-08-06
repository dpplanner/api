package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Comment;
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
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2 )
public class CommentRepositoryTest {


    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private TestEntityManager testEntityManager;


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

        if (parent != null) {
            parent.addChildren(comment);
        }

        return comment;
    }

    @BeforeEach
    public void setUp() {
        member = Member.builder().build();
        testEntityManager.persist(member);

        club = Club.builder().clubName("test").info("test").build();
        testEntityManager.persist(club);

        clubMember = clubMember.builder()
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

    }

    @Test
    public void CommentRepository_SaveReplyComment_ReturnSavedComment(){

        Comment parentComment = commentRepository.save(createComment(clubMember,post ,null));

        Comment savedReplyComment = commentRepository.save(createComment(clubMember,post ,parentComment));
        Comment savedReplyComment2 = commentRepository.save(createComment(clubMember, post, parentComment));


        assertThat(savedReplyComment).isNotNull();
        assertThat(savedReplyComment.getId()).isGreaterThan(0L);
        assertThat(savedReplyComment.getParent().getId()).isEqualTo(parentComment.getId());
        assertThat(parentComment.getChildren().size()).isEqualTo(2L);
    }

    @Test
    public void CommentRepository_findById_ReturnComment(){

        Comment comment = commentRepository.save(createComment(clubMember,post ,null));

        Comment foundComment = commentRepository.findById(comment.getId()).get();

        assertThat(foundComment).isNotNull();
        assertThat(foundComment.getId()).isEqualTo(comment.getId());
    }

    @Test
    public void CommentRepository_findByMemberId_ReturnComment() {

        Comment comment = commentRepository.save(createComment(clubMember,post ,null));

        Comment foundComment = commentRepository.findCommentByClubMemberId(clubMember.getId()).get();

        assertThat(foundComment).isNotNull();
        assertThat(foundComment.getId()).isEqualTo(comment.getId());
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
}