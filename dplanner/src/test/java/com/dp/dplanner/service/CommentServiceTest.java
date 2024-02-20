package com.dp.dplanner.service;

import com.dp.dplanner.domain.*;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.CommentDto;
import com.dp.dplanner.dto.CommentMemberLikeDto;
import com.dp.dplanner.dto.Status;
import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.CommentException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.CommentMemberLikeRepository;
import com.dp.dplanner.repository.CommentRepository;
import com.dp.dplanner.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static com.dp.dplanner.exception.ErrorResult.DELETE_AUTHORIZATION_DENIED;
import static com.dp.dplanner.exception.ErrorResult.UPDATE_AUTHORIZATION_DENIED;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Mock
    MessageService messageService;
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

    private List<Comment> createComments() {
        Comment comment = createComment(clubMember, post, null, "test1");
        Comment comment2 = createComment(clubMember, post, comment, "test2");
        Comment comment3 = createComment(clubMember, post, comment, "test3");
        Comment comment4 = createComment(clubMember, post, null, "test4");

        return Arrays.asList(comment, comment4, comment2, comment3);
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
        assertThat(createdComment.getClubMemberName()).isEqualTo("test");
        assertThat(createdComment.getLikeCount()).isEqualTo(0);
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

        doReturn(Optional.of(post)).when(postRepository).findById(postId);
        doReturn(Optional.of(clubMember)).when(clubMemberRepository).findById(clubMemberId);
        doReturn(createComments()).when(commentRepository).findCommentsUsingPostId(postId);
        List<CommentDto.Response> commentsList =  commentService.getCommentsByPostId(clubMemberId,postId);

        assertThat(commentsList.size()).isEqualTo(2);
        assertThat(commentsList.get(0).getChildren().size()).isEqualTo(2);
        assertThat(commentsList.get(1).getChildren().size()).isEqualTo(0);

        verify(commentMemberLikeRepository, times(4)).countDistinctByCommentId(any(Long.class));

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
        doReturn(Optional.of(clubMember)).when(clubMemberRepository).findById(clubMemberId);
        when(commentRepository.findCommentsByClubMemberId(clubMemberId)).thenReturn(Arrays.asList(comment, comment2, comment4));

        List<CommentDto.Response> commentsList = commentService.getCommentsByClubMemberId(clubMemberId, clubId);

        assertThat(commentsList).isNotNull();
        assertThat(commentsList.size()).isEqualTo(2);
        assertThat(commentsList.get(0).getChildren().size()).isEqualTo(1);
        assertThat(commentsList.get(0).getChildren().get(0).getClubMemberId()).isEqualTo(clubMemberId);
        assertThat(commentsList.get(0).getChildren().get(0).getPostId()).isEqualTo(postId);
        assertThat(commentsList.get(1).getParentId()).isEqualTo(newCommentId);
        assertThat(commentsList).extracting(CommentDto.Response::getClubMemberId).containsOnly(clubMemberId);
        assertThat(commentsList).extracting(CommentDto.Response::getPostId).containsOnly(postId);

        verify(commentMemberLikeRepository, times(3)).countDistinctByCommentId(any(Long.class));

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
    public void CommentService_UpdateComment_Throw_UPDATE_AUTHORIZATION_DENIED(){

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

        BaseException commentException = assertThrows(CommentException.class, () -> commentService.updateComment(clubMemberId, updateDto));
        assertThat(commentException.getErrorResult()).isEqualTo(UPDATE_AUTHORIZATION_DENIED);
    }

    @Test
    @DisplayName("자식 댓글 없는 댓글 삭제하는 경우")
    public void CommentService_DeleteComment_ReturnVoid_UsualCase() {

        Comment comment = createComment(clubMember, post, null, "test");
        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(comment));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        assertAll(() -> commentService.deleteComment(clubMemberId, commentId));

        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    @DisplayName("자식 댓글이 있는 댓글 삭제하는 경우")
    public void CommentService_DeleteComment_ReturnVoid_HasChildrenComment() {

        Comment comment = createComment(clubMember, post, null, "test");
        Comment child = createComment(clubMember, post, comment, "child");

        assert comment.getChildren().size() == 1;

        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(comment));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        assertAll(() -> commentService.deleteComment(clubMemberId, commentId));


        assertThat(comment.getIsDeleted()).isTrue();
        verify(commentRepository, times(0)).delete(comment);

    }
    @Test
    @DisplayName("삭제 하는 댓글의 부모 댓글이 isDeleted true이고 부모 댓글의 자식이 삭제하는 댓글 하나인 경우에는 둘 다 삭제")
    public void CommentService_DeleteComment_ReturnVoid_Recursive() {

        Comment comment = createComment(clubMember, post, null, "test");
        Comment child = createComment(clubMember, post, comment, "child");
        Long childId = child.getId();
        comment.delete();

        assert comment.getIsDeleted();
        assert comment.getChildren().size() == 1;

        when(commentRepository.findById(childId)).thenReturn(Optional.ofNullable(child));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        assertAll(() -> commentService.deleteComment(clubMemberId, childId));

        verify(commentRepository, times(1)).delete(comment); // child가 호출되는게 아니라 재귀적으로 parent가 삭제되고 orphan이 된 child가 삭제됨. 유닛테스트에서는 orphan이 삭제 되지 않음
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
    public void CommentService_DeleteComment_Throw_DELETE_AUTHORIZATION_DENIED() {
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

        BaseException commentException = assertThrows(CommentException.class, () -> commentService.deleteComment(usualClubMemberId, commentId));
        assertThat(commentException.getErrorResult()).isEqualTo(DELETE_AUTHORIZATION_DENIED);
    }

    @Test
    public void CommentService_LikeComment_ReturnCommentMemberLikeResponseDto(){

        Long commentMemberLikeId = 50L;
        Comment comment = createComment(clubMember, post, null, "test");
        ReflectionTestUtils.setField(comment, "club", club);
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
        ReflectionTestUtils.setField(comment, "club", club);
        CommentMemberLike commentMemberLike = CommentMemberLike.builder()
                .comment(comment)
                .clubMember(clubMember)
                .build();
        ReflectionTestUtils.setField(commentMemberLike,"id",commentMemberLikeId);

        doReturn(Optional.of(comment)).when(commentRepository).findById(commentId);
        doReturn(Optional.of(clubMember)).when(clubMemberRepository).findById(clubMemberId);
        when(commentMemberLikeRepository.findCommentMemberLikeByClubMemberIdAndCommentId(clubMemberId,commentId)).thenReturn(Optional.ofNullable(commentMemberLike));

        CommentMemberLikeDto.Response response = commentService.likeComment(clubMemberId,commentId);

        assertThat(response.getStatus()).isEqualTo(Status.DISLIKE);


    }
}
