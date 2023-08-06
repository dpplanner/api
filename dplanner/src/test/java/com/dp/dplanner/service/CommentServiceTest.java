package com.dp.dplanner.service;

import com.dp.dplanner.domain.Comment;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.CommentDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.CommentRepository;
import com.dp.dplanner.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private CommentService commentService;

    Member member;
    Post post;
    Club club;

    ClubMember clubMember;

    @BeforeEach
    public void setUp() {

        member = Member.builder()
                .build();

        ReflectionTestUtils.setField(member,"id",1L);

        club = Club.builder()
                .clubName("test")
                .info("test")
                .build();
        ReflectionTestUtils.setField(club,"id",1L);

        clubMember = ClubMember.builder()
                .club(club)
                .member(member)
                .name("test")
                .info("test")
                .build();


        post = Post.builder()
                .content("test")
                .club(club)
                .clubMember(clubMember)
                .isFixed(false)
                .build();

        ReflectionTestUtils.setField(post,"id",1L);

    }
    @Test
    public void CommentService_CreateComment_ReturnCommentResponseDto() {

        CommentDto.Create createDto = CommentDto.Create.builder()
                .content("test")
                .postId(1L)
                .parentId(null)
                .build();

        Comment comment = Comment.builder()
                .content("test")
                .parent(null)
                .clubMember(clubMember)
                .post(post)
                .build();

        ReflectionTestUtils.setField(comment,"id",1L);

        when(clubMemberRepository.findById(1L)).thenReturn(Optional.ofNullable(clubMember));
        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));
        when(commentRepository.save(Mockito.any(Comment.class))).thenReturn(comment);

        CommentDto.Response createdComment = commentService.createComment(1L, createDto);

        assertThat(createdComment).isNotNull();
        assertThat(createdComment.getParentId()).isNull();
        assertThat(createdComment.getId()).isEqualTo(1L);
        assertThat(createdComment.getClubMemberId()).isEqualTo(clubMember.getId());
        assertThat(createdComment.getContent()).isEqualTo("test");
        assertThat(createdComment.getChildren()).isEmpty();

    }

    @Test
    public void CommentService_CreateReplyComment_ReturnCommentResponseDto() {

        CommentDto.Create createDto = CommentDto.Create.builder()
                .content("test")
                .postId(1L)
                .parentId(1L)
                .build();

        Comment parent = Comment.builder()
                .content("parent")
                .parent(null)
                .clubMember(clubMember)
                .post(post)
                .build();
        ReflectionTestUtils.setField(parent, "id", 1L);

        Comment replyComment = Comment.builder()
                .content("test")
                .parent(parent)
                .clubMember(clubMember)
                .post(post)
                .build();
        ReflectionTestUtils.setField(replyComment,"id",2L);

        when(clubMemberRepository.findById(1L)).thenReturn(Optional.ofNullable(clubMember));
        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));
        when(commentRepository.findById(1L)).thenReturn(Optional.ofNullable(parent));
        when(commentRepository.save(Mockito.any(Comment.class))).thenReturn(replyComment);

        CommentDto.Response createdComment = commentService.createComment(1L, createDto);

        assertThat(createdComment).isNotNull();
        assertThat(createdComment.getId()).isEqualTo(2L);
        assertThat(createdComment.getParentId()).isEqualTo(1L);
        assertThat(createdComment.getClubMemberId()).isEqualTo(clubMember.getId());
        assertThat(createdComment.getContent()).isEqualTo("test");
        assertThat(createdComment.getChildren()).isEmpty();
        assertThat(parent.getChildren().size()).isEqualTo(1L);
        assertThat(parent.getChildren().get(0).getId()).isEqualTo(2L);
    }

    @Test
    public void CommentService_GetCommentByPostId_ReturnListCommentResponseDto(){

        Comment comment = Comment.builder().post(post).clubMember(clubMember).parent(null).content("test1").build();
        Comment comment2 = Comment.builder().post(post).clubMember(clubMember).parent(comment).content("test2").build();
        Comment comment3 = Comment.builder().post(post).clubMember(clubMember).parent(comment).content("test3").build();

        ReflectionTestUtils.setField(comment, "id", 1L);
        ReflectionTestUtils.setField(comment2, "id", 2L);
        ReflectionTestUtils.setField(comment3, "id", 3L);

        Comment comment4 = Comment.builder().post(post).clubMember(clubMember).parent(null).content("test4").build();
        ReflectionTestUtils.setField(comment4, "id", 4L);


        when(commentRepository.findCommentsUsingPostId(1L)).thenReturn(Arrays.asList(comment, comment4, comment2, comment3));

        List<CommentDto.Response> commentsList =  commentService.getCommentsByPostId(1L);

        assertThat(commentsList.size()).isEqualTo(2);
        assertThat(commentsList.get(0).getChildren().size()).isEqualTo(2);
        assertThat(commentsList.get(1).getChildren().size()).isEqualTo(0);

    }

}