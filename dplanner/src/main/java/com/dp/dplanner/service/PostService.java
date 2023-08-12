package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.PostDto;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.dto.PostDto.*;


@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMemberLikeRepository postMemberLikeRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberService clubMemberService;

    @Transactional(readOnly = true)
    public Response getPostById(long postId) {

        Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);

        return Response.of(post);

    }

    @Transactional(readOnly = true)
    public List<Response> getPostsByClubId(long clubId) {

        List<Post> posts = postRepository.findByClubId(clubId);
        return Response.ofList(posts);

    }

    @Transactional
    public Response createPost(long clubMemberId, Create create) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        Club club = clubRepository.findById(create.getClubId()).orElseThrow(RuntimeException::new);

        if (!clubMember.getClub().getId().equals(club.getId())) {
            throw new RuntimeException(); // 이중 확인
        }

        Post post = postRepository.save(create.toEntity(clubMember,club));

        return Response.of(post);
    }


    @Transactional
    public Response updatePost(long clubMemberId, Update update) {

        Post post = postRepository.findById(update.getId()).orElseThrow(RuntimeException::new);

        if (!post.getClubMember().getId().equals(clubMemberId)){
            throw new RuntimeException();
        }

        post.update(update);
        return Response.of(post);
    }


    @Transactional
    public void deletePostById(long clubMemberId, long postId) {

        Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        checkDeletable(post.getClubMember().getId(), clubMember);
        postRepository.delete(post);

    }

    private void checkDeletable(long clubMemberId, ClubMember clubMember) {

        if (!clubMember.getId().equals(clubMemberId)) {
            if(!clubMemberService.hasAuthority(clubMember.getId(), POST_ALL)){
                throw new RuntimeException();
            }
        }

    }


    @Transactional
    public PostMemberLikeDto.Response likePost(long clubMemberId,long postId) {

        Optional<PostMemberLike> find = postMemberLikeRepository.findPostMemberLikeByClubMemberIdAndPostId(clubMemberId,postId);

        if (find.isEmpty()) {

            Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);
            ClubMember clubMember =clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);

            PostMemberLike postMemberLike = postMemberLikeRepository.save(
                    PostMemberLike.builder()
                            .clubMember(clubMember)
                            .post(post)
                            .build()
            );

            return PostMemberLikeDto.Response.like(postMemberLike);
        }else{
            PostMemberLike postMemberLike = find.get();

            postMemberLikeRepository.delete(postMemberLike);

            return PostMemberLikeDto.Response.dislike(postMemberLike);
        }

    }


    @Transactional
    @RequiredAuthority(POST_ALL)
    public Response toggleIsFixed(long clubMemberId, long postId) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);
        post.toggleIsFixed();
        return Response.of(post);
    }

}

