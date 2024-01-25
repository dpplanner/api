package com.dp.dplanner.integration;

import com.dp.dplanner.TestConfig;
import com.dp.dplanner.domain.Comment;
import com.dp.dplanner.domain.CommentMemberLike;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.CommentDto;
import com.dp.dplanner.service.CommentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestConfig.class})
@Transactional
public class CommentServiceIntegrationTest {

    @Autowired
    CommentService commentService;

    @Autowired
    EntityManager entityManager;

    Long commentId = 0L;
    Club club;
    Member member;
    ClubMember clubMember;
    Post post;

    @BeforeEach
    public void setUp() {
        club = Club.builder().build();
        member = Member.builder().build();
        clubMember = ClubMember.builder().club(club).member(member).build();
        post = Post.builder().club(club).clubMember(clubMember).build();

        entityManager.persist(club);
        entityManager.persist(member);
        entityManager.persist(clubMember);
        entityManager.persist(post);
    }
    @Test
    public void CommentService_getCommentsByPostId_likeCountTest() {
        createComments();
        List<CommentDto.Response> commentsResponseList = commentService.getCommentsByPostId(clubMember.getId(),post.getId());

        assertThat(commentsResponseList.size()).isEqualTo(2);
        assertThat(commentsResponseList.get(0).getChildren().size()).isEqualTo(2);
        assertThat(commentsResponseList.get(1).getChildren().size()).isEqualTo(0);
        assertThat(commentsResponseList).extracting(CommentDto.Response::getLikeCount).isEqualTo(Arrays.asList(1, 0));
        assertThat(commentsResponseList.get(0).getChildren().get(0).getLikeCount()).isEqualTo(0);
        assertThat(commentsResponseList.get(0).getChildren().get(1).getLikeCount()).isEqualTo(1);
    }

    private Comment createComment(ClubMember clubMember, Post post, Comment parent, String content) {
        commentId += 1;
        Comment comment = Comment.builder()
                .clubMember(clubMember)
                .post(post)
                .parent(parent)
                .content(content)
                .build();

        entityManager.persist(comment);
        return comment;
    }
    private List<Comment> createComments() {
        Comment comment = createComment(clubMember, post, null, "test1");
        Comment comment2 = createComment(clubMember, post, comment, "test2");
        Comment comment3 = createComment(clubMember, post, comment, "test3");
        Comment comment4 = createComment(clubMember, post, null, "test4");

        entityManager.persist(CommentMemberLike.builder().comment(comment).clubMember(clubMember).build());
        entityManager.persist(CommentMemberLike.builder().comment(comment3).clubMember(clubMember).build());
        return Arrays.asList(comment, comment4, comment2, comment3);
    }
}
