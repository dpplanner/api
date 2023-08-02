package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.dto.PostDto;
import com.dp.dplanner.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceTests {

    @Mock
    private PostRepository postRepository;
    @InjectMocks
    private PostService postService;


    Member member;


    @BeforeEach
    public void setUp() {
        member = Member.builder()
                .build();

    }

    private Post createPost() {

        return Post.builder()
                .member(member)
                .content("test1")
                .build();
    }

    @Test
    public void PostService_CreatePost_ReturnPostResponseDto() {

        Post post = createPost();

        PostDto.Create createDto = PostDto.Create.builder().content("test").build();

        when(postRepository.save(Mockito.any(Post.class))).thenReturn(post);

        PostDto.Response createdPost = postService.createPost(createDto);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getContent()).isEqualTo("test");

    }


    @Test
    public void PostService_GetPostById_ReturnPostResponseDto() {

        Post post = createPost();

        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        PostDto.Response savedPost = postService.getPostById(1L);

        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getContent()).isEqualTo("test");
    }

    @Test
    public void PostService_GetPostById_ThrowError(){

        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(null));

        assertThatThrownBy(() -> postService.getPostById(1L))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    @Disabled
    public void PostService_UpdatePost_ReturnPostResponseDto() {

        Post post = createPost();

        PostDto.Update updateDto = PostDto.Update.builder()
                .id(1L)
                .content("update")
                .build();

        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        PostDto.Response updatedPost = postService.updatePost(updateDto,1L);

        assertThat(updatedPost).isNotNull();
        assertThat(updatedPost.getContent()).isEqualTo("update");

    }


    @Test
    public void PostService_DeletePostById_ReturnPostResponseDto() {

        Post post = createPost();

        when(postRepository.findById(1L)).thenReturn(Optional.ofNullable(post));

        assertAll(() -> postService.deletePostById(1L));

    }
}
