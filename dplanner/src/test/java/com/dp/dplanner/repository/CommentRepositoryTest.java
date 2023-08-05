package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Comment;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2 )
public class CommentRepositoryTest {


    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    Member member;
    Post post;
    Club club;

    private Comment createComment(Member member, Post post, Comment parent) {

        Comment comment = Comment.builder()
                .member(member)
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

        club = Club.builder().clubName("test").info("test").build();
        testEntityManager.persist(club);

        member = Member.builder().name("test").info("test").build();
        testEntityManager.persist(member);

        post = Post.builder().member(member).content("test").isFixed(false).build();
        testEntityManager.persist(post);

    }

    @Test
    public void CommentRepository_SaveComment_ReturnSavedComment(){

        Comment savedComment = commentRepository.save(createComment(member,post ,null));

        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getId()).isGreaterThan(0L);
        assertThat(savedComment.getParent()).isNull();

    }

    @Test
    public void CommentRepository_SaveReplyComment_ReturnSavedComment(){

        Comment parentComment = commentRepository.save(createComment(member,post ,null));

        Comment savedReplyComment = commentRepository.save(createComment(member,post ,parentComment));
        Comment savedReplyComment2 = commentRepository.save(createComment(member, post, parentComment));


        assertThat(savedReplyComment).isNotNull();
        assertThat(savedReplyComment.getId()).isGreaterThan(0L);
        assertThat(savedReplyComment.getParent().getId()).isEqualTo(parentComment.getId());
        assertThat(parentComment.getChildren().size()).isEqualTo(2L);
    }

    @Test
    public void CommentRepository_findById_ReturnComment(){

        Comment comment = commentRepository.save(createComment(member,post ,null));

        Comment foundComment = commentRepository.findById(comment.getId()).get();

        assertThat(foundComment).isNotNull();
        assertThat(foundComment.getId()).isEqualTo(comment.getId());
    }

    @Test
    public void CommentRepository_findByMemberId_ReturnComment() {

        Comment comment = commentRepository.save(createComment(member,post ,null));

        Comment foundComment = commentRepository.findCommentByMemberId(member.getId()).get();

        assertThat(foundComment).isNotNull();
        assertThat(foundComment.getId()).isEqualTo(comment.getId());
    }

    @Test
    public void CommentRepository_findByPostId_ReturnComments() {

        Comment comment = commentRepository.save(createComment(member,post ,null));
        Comment comment2 = commentRepository.save(createComment(member,post ,null));
        Comment comment3 = commentRepository.save(createComment(member,post ,comment));
        Comment comment4 = commentRepository.save(createComment(member,post ,comment));
        Comment comment5 = commentRepository.save(createComment(member,post ,comment2));

        List<Comment> commentsList = commentRepository.findCommentsUsingPostId(post.getId());

        assertThat(commentsList.size()).isEqualTo(5);
        assertThat(commentsList).extracting(Comment::getPost).extracting(Post::getId).containsOnly(post.getId());
    }

    @Test
    public void CommentRepository_delete_ReturnVoid() {

        Comment comment = commentRepository.save(createComment(member, post, null));
        Comment comment2 = commentRepository.save(createComment(member,post ,comment));

        commentRepository.delete(comment);

        assertThat(commentRepository.findById(comment.getId())).isEmpty();
        assertThat(commentRepository.findById(comment2.getId())).isEmpty();

    }
}
