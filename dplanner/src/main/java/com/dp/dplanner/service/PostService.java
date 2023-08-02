package com.dp.dplanner.service;

import com.dp.dplanner.domain.Post;
import com.dp.dplanner.dto.PostDto;
import com.dp.dplanner.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ToDo : ClubId 추가 , MemberId 추가

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public PostDto.Response getPostById(long postId) {

        Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);

        return PostDto.Response.of(post);

    }

/*    @Transactional(readOnly = true)
    public List<PostDto.Response> getPosts(long clubId) {


        List<Post> posts = postRepository.findAll();
        return PostDto.Response.ofList(posts);

    }*/

    @Transactional
    public PostDto.Response createPost(PostDto.Create create) {

        Post post = postRepository.save(create.toEntity());

        return PostDto.Response.of(post);
    }


    @Transactional
    public PostDto.Response updatePost(PostDto.Update update, long memberId) {

        Post post = postRepository.findById(update.getId()).orElseThrow(RuntimeException::new);

        if (!post.getMember().getId().equals(memberId))
            throw new RuntimeException();

            return PostDto.Response.of(post);
        }

    @Transactional
    public void deletePostById(long postId) {

        Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);
        postRepository.delete(post);

    }


}

