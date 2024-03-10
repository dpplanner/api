package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.AttachmentDto;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.dto.Status;
import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ServiceException;
import com.dp.dplanner.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Optional;

import static com.dp.dplanner.dto.PostDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    PostRepository postRepository;
    @Mock
    CommentRepository commentRepository;
    @Mock
    ClubRepository clubRepository;
    @Mock
    PostMemberLikeRepository postMemberLikeRepository;
    @Mock
    ClubMemberRepository clubMemberRepository;
    @Mock
    ClubMemberService clubMemberService;
    @Mock
    AttachmentService attachmentService;
    @Mock
    MessageService messageService;

    @InjectMocks
    private PostService postService;
    Member member;
    Club club;
    ClubMember clubMember;
    Post post;
    PostMemberLike postMemberLike;
    ClubMember adminMember;
    Long memberId;
    Long clubId;
    Long clubMemberId;
    Long adminMemberId;
    Long postId;
    Long postMemberLikeId;

    @BeforeEach
    public void setUp() {
        member = Member.builder()
                .build();
        memberId = 10L;
        ReflectionTestUtils.setField(member, "id", memberId);

        club = Club.builder()
                .clubName("testName")
                .info("test")
                .build();
        clubId = 20L;
        ReflectionTestUtils.setField(club, "id", clubId);

        clubMember = ClubMember.builder()
                .club(club)
                .member(member)
                .name("test")
                .info("test")
                .build();
        clubMemberId = 30L;
        ReflectionTestUtils.setField(clubMember, "id", clubMemberId);

        adminMember = ClubMember.builder()
                .club(club)
                .member(member)
                .name("test")
                .info("test")
                .build();
        adminMember.setAdmin();
        adminMemberId = 40L;
        ReflectionTestUtils.setField(adminMember, "id", adminMemberId);

        post = Post.builder()
                .clubMember(clubMember)
                .club(club)
                .isFixed(false)
                .content("test")
                .build();
        postId = 50L;
        ReflectionTestUtils.setField(post, "id", postId);

        postMemberLike = PostMemberLike.builder()
                .clubMember(clubMember)
                .post(post)
                .build();
        postMemberLikeId = 60L;
        ReflectionTestUtils.setField(postMemberLike, "id", postMemberLikeId);

    }

    private Post createPost() {
        Post post = Post.builder()
                .club(club)
                .clubMember(clubMember)
                .content("test")
                .build();
        ReflectionTestUtils.setField(post, "id", postId);

        return post;
    }

    private void assertResponse(Response response,String content) {
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(postId);
        assertThat(response.getContent()).isEqualTo(content);
        assertThat(response.getClubId()).isEqualTo(clubId);
        assertThat(response.getIsFixed()).isEqualTo(false);
        assertThat(response.getLikeCount()).isEqualTo(0);
        assertThat(response.getCommentCount()).isEqualTo(0);
        assertThat(response.getClubMemberName()).isEqualTo(clubMember.getName());
    }

    @Test
    public void PostService_CreatePost_ReturnPostResponseDto() throws Exception{

        String fileName = "testUpload";
        String contentType = "jpg";
        String filePath = "src/test/resources/test/testUpload.jpg";
        FileInputStream fileInputStream = new FileInputStream(filePath);
        MockMultipartFile multipartFile = new MockMultipartFile(fileName, fileName + "." + contentType, contentType, fileInputStream);

        Create createDto = Create.builder()
                .clubId(clubId)
                .content("test")
                .files(Arrays.asList(multipartFile))
                .build();

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(clubRepository.findById(clubId)).thenReturn(Optional.ofNullable(club));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(attachmentService.createAttachment(any(AttachmentDto.Create.class))).thenReturn(Arrays.asList(AttachmentDto.Response.builder().build()));

        Response response = postService.createPost(clubMemberId,createDto);

        assertResponse(response,"test");

    }


    @Test
    public void PostService_GetPostById_ReturnPostResponseDto() {
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(commentRepository.countDistinctByPostId(postId)).thenReturn(0L);
        when(postMemberLikeRepository.countDistinctByPostId(postId)).thenReturn(0L);

        Response response = postService.getPostById(clubMemberId, postId);

        assertResponse(response,"test");

        verify(postRepository, times(1)).findById(postId);
        verify(commentRepository, times(1)).countDistinctByPostId(postId);
        verify(postMemberLikeRepository, times(1)).countDistinctByPostId(postId);

    }

    @Test
    public void PostService_GetPostById_Throw_POST_NOT_FOUND() {
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        BaseException postException = assertThrows(ServiceException.class, () -> postService.getPostById(clubMemberId, postId));
        assertThat(postException.getErrorResult()).isEqualTo(POST_NOT_FOUND);
    }

    @Test
    public void PostService_GetPostById_Throw_Error_CLUBMEMBER_NOT_FOUND(){
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.empty());

        BaseException clubMemberException = assertThrows(ServiceException.class, () -> postService.getPostById(clubMemberId, postId));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(CLUBMEMBER_NOT_FOUND);

    }

    @Test
    public void PostService_GetPostById_Throw_Error_DIFFERENT_CLUB_EXCEPTION(){

        Club newClub = Club.builder().build();
        ReflectionTestUtils.setField(newClub, "id", clubId + 1);

        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        ReflectionTestUtils.setField(post, "club", newClub);
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));

        BaseException clubMemberException = assertThrows(ServiceException.class, () -> postService.getPostById(clubMemberId, postId));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(DIFFERENT_CLUB_EXCEPTION);

    }

    @Test
    public void PostService_GetPostByClubId_ReturnSliceResponseDto() {

        Post post1 = createPost();
        Post post2 = createPost();
        Post post3 = createPost();
        Post post4 = createPost();
        Post post5 = createPost();

        Object[] postObject1 = {post1, null, 0L, 0L};
        Object[] postObject2 = {post2, null, 0L, 0L};
        Object[] postObject3 = {post3, null, 0L, 0L};
        Object[] postObject4 = {post4, null, 0L, 0L};
        Object[] postObject5 = {post5, null, 0L, 0L};

        Slice<Object[]> postSlice = new SliceImpl<>(Arrays.asList(postObject1, postObject2, postObject3, postObject4, postObject5), PageRequest.of(0, 10), false);

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(postRepository.findByClubId(clubId,clubMemberId ,PageRequest.of(0,10))).thenReturn(postSlice);

        SliceResponse responseSlice = postService.getPostsByClubId(clubMemberId, clubId, PageRequest.of(0, 10));


        assertThat(responseSlice).isNotNull();
        assertThat(responseSlice.getContent().size()).isEqualTo(5); // # of elements
        assertThat(responseSlice.getPageable().getPageNumber()).isEqualTo(0); // current page
        assertThat(responseSlice.getPageable().getPageSize()).isEqualTo(10); // size
        assertThat(responseSlice.isHasNext()).isFalse();
        assertThat(responseSlice.getContent()).extracting(Response::getClubId).containsOnly(clubId);
        responseSlice.getContent().forEach(response -> assertResponse(response, "test"));

    }

    @Test
    public void PostService_UpdatePost_ReturnPostResponseDto() {

        Update updateDto = Update.builder()
                .id(postId)
                .content("update")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));

        Response response = postService.updatePost(clubMemberId, updateDto);

        assertResponse(response, "update");

    }

    @Test
    public void PostService_UpdatePost_Throw_UPDATE_AUTHORIZATION_DENIED(){

        Update updateDto = Update.builder()
                .id(postId)
                .content("update")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));

        BaseException postException = assertThrows(ServiceException.class, () -> postService.updatePost(clubMemberId+1, updateDto));
        assertThat(postException.getErrorResult()).isEqualTo(UPDATE_AUTHORIZATION_DENIED);

    }


    @Test
    public void PostService_DeletePostByUser_ReturnVoid() {
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));

        assertAll(() -> postService.deletePostById(clubMemberId, postId));
    }

    @Test
    public void PostService_DeletePostByAdmin_ReturnVoid() {

        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(adminMember));
        when(clubMemberService.hasAuthority(adminMember.getId(), ClubAuthorityType.POST_ALL)).thenReturn(true);
        assertAll(() -> postService.deletePostById(clubMemberId,postId));

    }

    @Test
    @DisplayName("권한이 없는 클럽 맴버가 삭제하면 DELETE_AUTHORIZATION_DENIED를 던짐")
    public void PostService_DeletePostByAdmin_Throw_DELETE_AUTHORIZATION_DENIED() {

        Member newMember = Member.builder().build();
        ClubMember newClubMember = ClubMember.builder()
                .member(newMember)
                .club(club)
                .build();
        ReflectionTestUtils.setField(newClubMember, "id", clubMemberId + 1);
        ReflectionTestUtils.setField(post,"clubMember",newClubMember);

        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(clubMemberService.hasAuthority(clubMember.getId(), ClubAuthorityType.POST_ALL)).thenReturn(false);

        BaseException postException = assertThrows(ServiceException.class, () -> postService.deletePostById(clubMemberId, postId));
        assertThat(postException.getErrorResult()).isEqualTo(DELETE_AUTHORIZATION_DENIED);
    }

    @Test
    public void PostService_LikePost_ReturnPostMemberLikeResponseDto(){

        when(postMemberLikeRepository.findByClubMemberIdAndPostId(clubMemberId,postId)).thenReturn(Optional.empty());
        when(postMemberLikeRepository.save(any(PostMemberLike.class))).thenReturn(postMemberLike);
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));

        PostMemberLikeDto.Response response = postService.likePost(clubMemberId,postId);

        assertThat(response.getStatus()).isEqualTo(Status.LIKE);

    }

    @Test
    public void PostService_DisLikePost_ReturnPostMemberLikeResponseDto(){


        when(postMemberLikeRepository.findByClubMemberIdAndPostId(clubMemberId,postId)).thenReturn(Optional.ofNullable(postMemberLike));

        PostMemberLikeDto.Response response = postService.likePost(clubMemberId,postId);

        assertThat(response.getStatus()).isEqualTo(Status.DISLIKE);


    }

    @Test
    public void PostService_FixPost_ReturnPostResponseDto() {
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));

        Response fixedPost = postService.toggleIsFixed(clubMemberId,postId);

        assertThat(fixedPost).isNotNull();
        assertThat(fixedPost.getIsFixed()).isTrue();
    }

    @Test
    public void PostService_UnFixPost_ReturnPostResponseDto(){
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        ReflectionTestUtils.setField(post,"isFixed",true);

        Response unfixedPost = postService.toggleIsFixed(clubMemberId,postId);

        assertThat(unfixedPost).isNotNull();
        assertThat(unfixedPost.getIsFixed()).isFalse();
    }

}
