package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.PostDto;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.dto.Status;
import com.dp.dplanner.repository.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private PostMemberLikeRepository postMemberLikeRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

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
                .clubName("test")
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

        return Post.builder()
                .club(club)
                .clubMember(clubMember)
                .content("test")
                .build();
    }

    @Test
    public void PostService_CreatePost_ReturnPostResponseDto() {


        PostDto.Create createDto = PostDto.Create.builder().clubId(clubId).content("test").build();

        when(postRepository.save(Mockito.any(Post.class))).thenReturn(post);
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(clubRepository.findById(clubId)).thenReturn(Optional.ofNullable(club));

        PostDto.Response createdPost = postService.createPost(clubMemberId,createDto);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getId()).isEqualTo(postId);
        assertThat(createdPost.getContent()).isEqualTo("test");
        assertThat(createdPost.getClubId()).isEqualTo(clubId);
        assertThat(createdPost.getIsFixed()).isEqualTo(false);

    }


    @Test
    public void PostService_GetPostById_ReturnPostResponseDto() {


        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));

        PostDto.Response foundPost = postService.getPostById(postId);

        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getId()).isEqualTo(postId);
        assertThat(foundPost.getContent()).isEqualTo("test");
        assertThat(foundPost.getClubId()).isEqualTo(clubId);
        assertThat(foundPost.getIsFixed()).isEqualTo(false);
    }

    @Test
    public void PostService_GetPostById_ThrowError(){

        when(postRepository.findById(clubId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(clubId))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    public void PostService_GetPostByClubId_ReturnListPostResponseDto() {

        Post post1 = createPost();
        Post post2 = createPost();
        List<Post> postList = Arrays.asList(post1, post2);

        when(postRepository.findByClubId(clubId)).thenReturn(postList);

        List<PostDto.Response> posts = postService.getPostsByClubId(clubId);

        assertThat(posts).isNotNull();
        assertThat(posts.size()).isEqualTo(2);
        assertThat(posts).extracting(PostDto.Response :: getClubId).containsOnly(clubId);

    }

    @Test
    public void PostService_UpdatePost_ReturnPostResponseDto() {


        PostDto.Update updateDto = PostDto.Update.builder()
                .id(postId)
                .content("update")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));

        PostDto.Response updatedPost = postService.updatePost(clubMemberId, updateDto);

        assertThat(updatedPost).isNotNull();
        assertThat(updatedPost.getId()).isEqualTo(postId);
        assertThat(updatedPost.getContent()).isEqualTo("update");

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

        assertAll(() -> postService.deletePostById(clubMemberId,postId));

    }

    @Test
    public void PostService_LikePost_ReturnPostMemberLikeResponseDto(){

        when(postMemberLikeRepository.findPostMemberLikeByClubMemberIdAndPostId(clubMemberId,postId)).thenReturn(Optional.empty());
        when(postMemberLikeRepository.save(Mockito.any(PostMemberLike.class))).thenReturn(postMemberLike);
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));

        PostMemberLikeDto.Response response = postService.likePost(clubMemberId,postId);

        assertThat(response.getStatus()).isEqualTo(Status.LIKE);

    }

    @Test
    public void PostService_DisLikePost_ReturnPostMemberLikeResponseDto(){


        when(postMemberLikeRepository.findPostMemberLikeByClubMemberIdAndPostId(clubMemberId,postId)).thenReturn(Optional.ofNullable(postMemberLike));

        PostMemberLikeDto.Response response = postService.likePost(clubMemberId,postId);

        assertThat(response.getStatus()).isEqualTo(Status.DISLIKE);


    }

    @Test
    public void PostService_FixPost_ReturnPostResponseDto() {
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(adminMember));
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));

        PostDto.Response fixedPost = postService.toggleIsFixed(clubMemberId,postId);

        assertThat(fixedPost).isNotNull();
        assertThat(fixedPost.getIsFixed()).isTrue();
    }

    @Test
    public void PostService_UnFixPost_ReturnPostResponseDto(){
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(adminMember));
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        ReflectionTestUtils.setField(post,"isFixed",true);

        PostDto.Response unfixedPost = postService.toggleIsFixed(clubMemberId,postId);

        assertThat(unfixedPost).isNotNull();
        assertThat(unfixedPost.getIsFixed()).isFalse();
    }

}
