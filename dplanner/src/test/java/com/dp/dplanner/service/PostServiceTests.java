package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.dto.PostDto;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.repository.PostMemberLikeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceTests {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostMemberLikeRepository postMemberLikeRepository;

    @InjectMocks
    private PostService postService;
    Member member;
    Club club;

    Post post;
    PostMemberLike postMemberLike;

    @BeforeEach
    public void setUp() {
        member = Member.builder()
                .name("test")
                .info("test")
                .build();

        ReflectionTestUtils.setField(member, "id", 1L);

        club = Club.builder()
                .clubName("test")
                .info("test")
                .build();

        ReflectionTestUtils.setField(club, "id", 1L);

        post = Post.builder()
                .member(member)
                .club(club)
                .content("test")
                .build();

        ReflectionTestUtils.setField(post, "id", 1L);

        postMemberLike = PostMemberLike.builder()
                .member(member)
                .post(post)
                .build();

        ReflectionTestUtils.setField(postMemberLike, "id", 1L);
    }

    private Post createPost() {

        return Post.builder()
                .member(member)
                .club(club)
                .content("test")
                .build();
    }

    @Test
    public void PostService_CreatePost_ReturnPostResponseDto() {


        PostDto.Create createDto = PostDto.Create.builder().content("test").build();

        when(postRepository.save(Mockito.any(Post.class))).thenReturn(post);

        PostDto.Response createdPost = postService.createPost(createDto);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getId()).isGreaterThan(0L);
        assertThat(createdPost.getContent()).isEqualTo("test");

    }


    @Test
    public void PostService_GetPostById_ReturnPostResponseDto() {


        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        PostDto.Response foundPost = postService.getPostById(1L);

        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getId()).isGreaterThan(0L);
        assertThat(foundPost.getContent()).isEqualTo("test");
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

        PostDto.Response updatedPost = postService.updatePost(updateDto,1L);

        assertThat(updatedPost).isNotNull();
        assertThat(updatedPost.getId()).isGreaterThan(0L);
        assertThat(updatedPost.getContent()).isEqualTo("update");

    }


    @Test
    public void PostService_DeletePostById_ReturnPostResponseDto() {


        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        assertAll(() -> postService.deletePostById(1L,1L));

    }

    @Test
    public void PostService_LikePost_ReturnResponseDto(){

        when(postMemberLikeRepository.findPostMemberLikeByMemberIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postMemberLikeRepository.save(Mockito.any(PostMemberLike.class))).thenReturn(postMemberLike);
        when(memberRepository.findById(1L)).thenReturn(Optional.ofNullable(member));
        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        PostMemberLikeDto.Response response = postService.likePost(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(PostMemberLikeDto.Status.LIKE);

    }

    @Test
    public void PostService_DisLikePost_ReturnResponseDto(){


        when(postMemberLikeRepository.findPostMemberLikeByMemberIdAndPostId(1L, 1L)).thenReturn(Optional.ofNullable(postMemberLike));

        PostMemberLikeDto.Response response = postService.likePost(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(PostMemberLikeDto.Status.DISLIKE);


    }
}
