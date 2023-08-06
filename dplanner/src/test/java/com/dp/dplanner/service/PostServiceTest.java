package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.PostDto;
import com.dp.dplanner.dto.PostMemberLikeDto;
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
    private MemberRepository memberRepository;
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

    @BeforeEach
    public void setUp() {
        member = Member.builder()
                .build();

        ReflectionTestUtils.setField(member, "id", 1L);

        club = Club.builder()
                .clubName("test")
                .info("test")
                .build();

        ReflectionTestUtils.setField(club, "id", 1L);

        clubMember = ClubMember.builder()
                .club(club)
                .member(member)
                .name("test")
                .info("test")
                .build();

        ReflectionTestUtils.setField(clubMember, "id", 1L);

        adminMember = ClubMember.builder()
                .club(club)
                .member(member)
                .name("test")
                .info("test")
                .build();
        adminMember.setAdmin();
        ReflectionTestUtils.setField(adminMember, "id", 1L);

        post = Post.builder()
                .clubMember(clubMember)
                .club(club)
                .isFixed(false)
                .content("test")
                .build();

        ReflectionTestUtils.setField(post, "id", 1L);

        postMemberLike = PostMemberLike.builder()
                .clubMember(clubMember)
                .post(post)
                .build();

        ReflectionTestUtils.setField(postMemberLike, "id", 1L);




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


        PostDto.Create createDto = PostDto.Create.builder().clubId(1L).content("test").build();

        when(postRepository.save(Mockito.any(Post.class))).thenReturn(post);
        when(clubMemberRepository.findById(1L)).thenReturn(Optional.ofNullable(clubMember));
        when(clubRepository.findById(1L)).thenReturn(Optional.ofNullable(club));

        PostDto.Response createdPost = postService.createPost(1L,createDto);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getId()).isGreaterThan(0L);
        assertThat(createdPost.getContent()).isEqualTo("test");
        assertThat(createdPost.getClubId()).isEqualTo(1L);
        assertThat(createdPost.getIsFixed()).isEqualTo(false);

    }


    @Test
    public void PostService_GetPostById_ReturnPostResponseDto() {


        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        PostDto.Response foundPost = postService.getPostById(1L);

        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getId()).isGreaterThan(0L);
        assertThat(foundPost.getContent()).isEqualTo("test");
        assertThat(foundPost.getClubId()).isEqualTo(1L);
        assertThat(foundPost.getIsFixed()).isEqualTo(false);
    }

    @Test
    public void PostService_GetPostById_ThrowError(){

        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(1L))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    public void PostService_GetPostByClubId_ReturnListPostResponseDto() {

        Post post1 = createPost();
        Post post2 = createPost();
        List<Post> postList = Arrays.asList(post1, post2);

        when(postRepository.findByClubId(1L)).thenReturn(postList);

        List<PostDto.Response> posts = postService.getPostsByClubId(1L);

        assertThat(posts).isNotNull();
        assertThat(posts.size()).isEqualTo(2);
        assertThat(posts).extracting(PostDto.Response :: getClubId).containsOnly(1L);

    }

    @Test
    public void PostService_UpdatePost_ReturnPostResponseDto() {


        PostDto.Update updateDto = PostDto.Update.builder()
                .id(1L)
                .content("update")
                .build();

        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        PostDto.Response updatedPost = postService.updatePost(1L, updateDto);

        assertThat(updatedPost).isNotNull();
        assertThat(updatedPost.getId()).isGreaterThan(0L);
        assertThat(updatedPost.getContent()).isEqualTo("update");

    }


    @Test
    public void PostService_DeletePostByUser_ReturnVoid() {
        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));
        when(clubMemberRepository.findById(1L)).thenReturn(Optional.ofNullable(clubMember));

        assertAll(() -> postService.deletePostById(1L, 1L));
    }

    @Test
    public void PostService_DeletePostByAdmin_ReturnVoid() {

        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));
        when(clubMemberRepository.findById(1L)).thenReturn(Optional.ofNullable(adminMember));

        assertAll(() -> postService.deletePostById(1L, 1L));

    }

    @Test
    public void PostService_LikePost_ReturnResponseDto(){

        when(postMemberLikeRepository.findPostMemberLikeByClubMemberIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postMemberLikeRepository.save(Mockito.any(PostMemberLike.class))).thenReturn(postMemberLike);
        when(clubMemberRepository.findById(1L)).thenReturn(Optional.ofNullable(clubMember));
        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        PostMemberLikeDto.Response response = postService.likePost(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(PostMemberLikeDto.Status.LIKE);

    }

    @Test
    public void PostService_DisLikePost_ReturnResponseDto(){


        when(postMemberLikeRepository.findPostMemberLikeByClubMemberIdAndPostId(1L, 1L)).thenReturn(Optional.ofNullable(postMemberLike));

        PostMemberLikeDto.Response response = postService.likePost(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(PostMemberLikeDto.Status.DISLIKE);


    }

    @Test
    public void PostService_FixPost_ReturnPostResponseDto() {
        when(clubMemberRepository.findById(1L)).thenReturn(Optional.ofNullable(adminMember));
        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        PostDto.Response fixedPost = postService.toggleIsFixed(1L,1L);

        assertThat(fixedPost).isNotNull();
        assertThat(fixedPost.getIsFixed()).isTrue();
    }

    @Test
    public void PostService_UnFixPost_ReturnPostResponseDto(){
        when(clubMemberRepository.findById(1L)).thenReturn(Optional.ofNullable(adminMember));
        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));
        ReflectionTestUtils.setField(post,"isFixed",true);

        PostDto.Response unfixedPost = postService.toggleIsFixed(1L, 1L);

        assertThat(unfixedPost).isNotNull();
        assertThat(unfixedPost.getIsFixed()).isFalse();
    }

}
