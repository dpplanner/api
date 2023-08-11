package com.dp.dplanner.service;

import com.dp.dplanner.domain.*;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.CommentDto;
import com.dp.dplanner.dto.CommentMemberLikeDto;
import com.dp.dplanner.dto.Status;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.CommentMemberLikeRepository;
import com.dp.dplanner.repository.CommentRepository;
import com.dp.dplanner.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
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

import static com.dp.dplanner.domain.club.ClubAuthorityType.POST_ALL;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    CommentRepository commentRepository;
    @Mock
    PostRepository postRepository;
    @Mock
    ClubMemberRepository clubMemberRepository;
    @Mock
    ClubMemberService clubMemberService;
    @Mock
    CommentMemberLikeRepository commentMemberLikeRepository;
    @InjectMocks
    CommentService commentService;

    Member member;
    Post post;
    Club club;
    ClubMember clubMember;

    Long memberId;
    Long clubId;
    Long clubMemberId;
    Long postId;
    Long commentId = 0L;

    private Comment createComment(ClubMember clubMember,Post post,Comment parent, String content) {
        commentId += 1;
        Comment comment = Comment.builder()
                .clubMember(clubMember)
                .post(post)
                .parent(parent)
                .content(content)
                .build();
        ReflectionTestUtils.setField(comment, "id", commentId);
        return comment;
    }

    @BeforeEach
    public void setUp() {

        member = Member.builder()
                .build();
        memberId = 10L;

        ReflectionTestUtils.setField(member,"id",memberId);

        club = Club.builder()
                .clubName("test")
                .info("test")
                .build();
        clubId = 20L;
        ReflectionTestUtils.setField(club,"id",clubId);

        clubMember = ClubMember.builder()
                .club(club)
                .member(member)
                .name("test")
                .info("test")
                .build();
        clubMemberId = 30L;

        ReflectionTestUtils.setField(clubMember,"id",clubMemberId);

        post = Post.builder()
                .content("test")
                .club(club)
                .clubMember(clubMember)
                .isFixed(false)
                .build();
        postId = 40L;
        ReflectionTestUtils.setField(post,"id",postId);


    }

    @AfterEach
    public void clear() {
        commentId = 0L;
    }
    @Test
    public void CommentService_CreateComment_ReturnCommentResponseDto() {

        CommentDto.Create createDto = CommentDto.Create.builder()
                .content("test")
                .postId(postId)
                .parentId(null)
                .build();

        Comment comment = createComment(clubMember, post, null,"test");

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        when(commentRepository.save(Mockito.any(Comment.class))).thenReturn(comment);

        CommentDto.Response createdComment = commentService.createComment(clubMemberId, createDto);

        assertThat(createdComment).isNotNull();
        assertThat(createdComment.getParentId()).isNull();
        assertThat(createdComment.getId()).isEqualTo(commentId);
        assertThat(createdComment.getClubMemberId()).isEqualTo(clubMember.getId());
        assertThat(createdComment.getContent()).isEqualTo("test");
        assertThat(createdComment.getChildren()).isEmpty();

    }


    @Test
    public void CommentService_CreateReplyComment_ReturnCommentResponseDto() {

        Comment parent = createComment(clubMember, post, null, "parent");
        Long parentId = parent.getId();

        CommentDto.Create createDto = CommentDto.Create.builder()
                .content("test")
                .postId(postId)
                .parentId(parentId)
                .build();

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        when(commentRepository.findById(parentId)).thenReturn(Optional.ofNullable(parent));
        when(commentRepository.save(Mockito.any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentDto.Response createdComment = commentService.createComment(clubMemberId, createDto);

        assertThat(createdComment).isNotNull();
        assertThat(createdComment.getParentId()).isEqualTo(parentId);
        assertThat(createdComment.getClubMemberId()).isEqualTo(clubMember.getId());
        assertThat(createdComment.getContent()).isEqualTo("test");
        assertThat(createdComment.getChildren()).isEmpty();
        assertThat(parent.getChildren().size()).isEqualTo(1);
        assertThat(parent.getChildren().get(0).getContent()).isEqualTo("test");
    }



    @Test
    public void CommentService_GetCommentsByPostId_ReturnListCommentResponseDto(){

//        when(commentRepository.findCommentsUsingPostId(postId)).thenReturn(createComments());
        doReturn(createComments()).when(commentRepository).findCommentsUsingPostId(postId);
        List<CommentDto.Response> commentsList =  commentService.getCommentsByPostId(postId);

        assertThat(commentsList.size()).isEqualTo(2);
        assertThat(commentsList.get(0).getChildren().size()).isEqualTo(2);
        assertThat(commentsList.get(1).getChildren().size()).isEqualTo(0);

    }

    private List<Comment> createComments() {
        Comment comment = createComment(clubMember, post, null, "test1");
        Comment comment2 = createComment(clubMember, post, comment, "test2");
        Comment comment3 = createComment(clubMember, post, comment, "test3");
        Comment comment4 = createComment(clubMember, post, null, "test4");

        return Arrays.asList(comment, comment4, comment2, comment3);
    }
    @Test
    public void CommentService_GetCommentsByClubMemberId_ReturnListCommentResponseDto() {

        Member newMember = Member.builder().build();
        ClubMember newClubMember =  ClubMember.builder()
                .club(club)
                .member(newMember)
                .name("test")
                .info("test")
                .build();
        ReflectionTestUtils.setField(newMember,"id",memberId+1);
        ReflectionTestUtils.setField(newClubMember,"id",clubMemberId+1);

        Comment comment = createComment(clubMember, post, null, "test1");
        Comment comment2 = createComment(clubMember, post, comment, "test2");
        Comment comment3 = createComment(newClubMember, post,null, "test3");
        Comment comment4 = createComment(clubMember, post,comment3, "test4");

        Long newCommentId = comment3.getId();

        when(commentRepository.findCommentsByClubMemberId(clubMemberId)).thenReturn(Arrays.asList(comment, comment2, comment4));

        List<CommentDto.Response> commentsList = commentService.getCommentsByClubMemberId(clubMemberId);

        assertThat(commentsList).isNotNull();
        assertThat(commentsList.size()).isEqualTo(2);
        assertThat(commentsList.get(0).getChildren().size()).isEqualTo(1);
        assertThat(commentsList.get(0).getChildren().get(0).getClubMemberId()).isEqualTo(clubMemberId);
        assertThat(commentsList.get(0).getChildren().get(0).getPostId()).isEqualTo(postId);
        assertThat(commentsList.get(1).getParentId()).isEqualTo(newCommentId);
        assertThat(commentsList).extracting(CommentDto.Response::getClubMemberId).containsOnly(clubMemberId);
        assertThat(commentsList).extracting(CommentDto.Response::getPostId).containsOnly(postId);
    }

    @Test
    public void CommentService_UpdateComment_ReturnCommentResponseDto(){
        Comment comment = createComment(clubMember,post,null,"test");

        CommentDto.Update updateDto = CommentDto.Update.builder()
                .id(commentId)
                .content("update")
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(comment));

        CommentDto.Response updatedComment = commentService.updateComment(clubMemberId, updateDto);

        assertThat(updatedComment).isNotNull();
        assertThat(updatedComment.getPostId()).isEqualTo(postId);
        assertThat(updatedComment.getClubMemberId()).isEqualTo(clubMemberId);
        assertThat(updatedComment.getContent()).isEqualTo("update");

    }

    @Test
    public void CommentService_UpdateComment_ThrowException(){

        Member newMember = Member.builder().build();
        ClubMember newClubMember = ClubMember.builder()
                .name("different")
                .member(newMember)
                .club(club)
                .build();
        ReflectionTestUtils.setField(newClubMember, "id", clubMemberId + 1);


        Comment comment =  createComment(newClubMember,post,null,"test");

        CommentDto.Update updateDto = CommentDto.Update.builder()
                .id(commentId)
                .content("update")
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(comment));

        assertThatThrownBy(() -> commentService.updateComment(clubMemberId, updateDto))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void CommentService_DeleteComment_ReturnVoid_UsualCase() {

        Comment comment = createComment(clubMember, post, null, "test");
        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(comment));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        assertAll(() -> commentService.deleteComment(clubMemberId, commentId));
    }

    @Test
    public void CommentService_DeleteComment_ReturnVoid_UsualCaseAdmin() {
        Long adminMemberId = clubMemberId + 1;
        Member adminMember = Member.builder().build();
        ClubMember adminClubMember = ClubMember.builder()
                .member(adminMember)
                .club(club)
                .build();
        ReflectionTestUtils.setField(adminClubMember, "id", adminMemberId);
        adminClubMember.setAdmin();

        Comment comment = createComment(clubMember, post, null, "test");
        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(comment));
        when(clubMemberRepository.findById(adminMemberId)).thenReturn(Optional.ofNullable(adminClubMember));
        when(clubMemberService.hasAuthority(adminClubMember.getId(), POST_ALL)).thenReturn(true);

        assertAll(() -> commentService.deleteComment(adminMemberId, commentId));

    }

    @Test
    public void CommentService_DeleteComment_ThrowException() {
        Long usualClubMemberId = clubMemberId + 1;
        Member usualMember = Member.builder().build();
        ClubMember usualClubMember = ClubMember.builder()
                .member(usualMember)
                .club(club)
                .build();
        ReflectionTestUtils.setField(usualClubMember, "id", usualClubMemberId);

        Comment comment = createComment(clubMember, post, null, "test");
        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(comment));
        when(clubMemberRepository.findById(usualClubMemberId)).thenReturn(Optional.ofNullable(usualClubMember));
        when(clubMemberService.hasAuthority(usualClubMemberId, POST_ALL)).thenReturn(false);

        assertThatThrownBy(() -> commentService.deleteComment(usualClubMemberId, commentId))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    public void CommentService_LikeComment_ReturnCommentMemberLikeResponseDto(){

        Long commentMemberLikeId = 50L;
        Comment comment = createComment(clubMember, post, null, "test");
        CommentMemberLike commentMemberLike = CommentMemberLike.builder()
                .comment(comment)
                .clubMember(clubMember)
                .build();
        ReflectionTestUtils.setField(commentMemberLike,"id",commentMemberLikeId);

        when(commentMemberLikeRepository.findCommentMemberLikeByClubMemberIdAndCommentId(clubMemberId,commentId)).thenReturn(Optional.empty());
        when(commentMemberLikeRepository.save(Mockito.any(CommentMemberLike.class))).thenReturn(commentMemberLike);
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(comment));

        CommentMemberLikeDto.Response response = commentService.likeComment(clubMemberId,commentId);

        assertThat(response.getStatus()).isEqualTo(Status.LIKE);

    }

    @Test
    public void CommentService_DisLikeComment_ReturnCommentMemberLikeResponseDto(){

        Long commentMemberLikeId = 50L;
        Comment comment = createComment(clubMember, post, null, "test");
        CommentMemberLike commentMemberLike = CommentMemberLike.builder()
                .comment(comment)
                .clubMember(clubMember)
                .build();
        ReflectionTestUtils.setField(commentMemberLike,"id",commentMemberLikeId);

        when(commentMemberLikeRepository.findCommentMemberLikeByClubMemberIdAndCommentId(clubMemberId,commentId)).thenReturn(Optional.ofNullable(commentMemberLike));

        CommentMemberLikeDto.Response response = commentService.likeComment(clubMemberId,commentId);

        assertThat(response.getStatus()).isEqualTo(Status.DISLIKE);


    }
}
