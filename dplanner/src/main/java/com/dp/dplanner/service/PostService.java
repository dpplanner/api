package com.dp.dplanner.service;

import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.club.ClubRole;
import com.dp.dplanner.dto.PostDto;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMemberLikeRepository postMemberLikeRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public PostDto.Response getPostById(long postId) {

        Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);

        return PostDto.Response.of(post);

    }

    @Transactional(readOnly = true)
    public List<PostDto.Response> getPostsByClubId(long clubId) {

        List<Post> posts = postRepository.findByClubId(clubId);
        return PostDto.Response.ofList(posts);

    }

    @Transactional
    public PostDto.Response createPost(long clubMemberId, PostDto.Create create) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        Club club = clubRepository.findById(create.getClubId()).orElseThrow(RuntimeException::new);

        if (!clubMember.getClub().getId().equals(club.getId())) {
            throw new RuntimeException(); // 이중 확인
        }

        Post post = postRepository.save(create.toEntity(clubMember,club));

        return PostDto.Response.of(post);
    }


    @Transactional
    public PostDto.Response updatePost(long clubMemberId, PostDto.Update update) {

        Post post = postRepository.findById(update.getId()).orElseThrow(RuntimeException::new);

        if (!post.getClubMember().getId().equals(clubMemberId)){
            throw new RuntimeException();
        }

        post.update(update);
        return PostDto.Response.of(post);
    }


    @Transactional
    public void deletePostById(long clubMemberId, long postId) {

        Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        checkDeletable(post.getClubMember().getId(), clubMember);
        postRepository.delete(post);

    }

    /**
     *  추후에 검사 기능 추가
     */
    private void checkDeletable(long clubMemberId, ClubMember clubMember) {

        if (! (clubMember.getId().equals(clubMemberId) || clubMember.getRole().equals(ClubRole.ADMIN))){
            throw new RuntimeException(); // 권한 X
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
    public PostDto.Response toggleIsFixed(long clubMemberId, long postId) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        checkFixable(clubMember);
        Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);
        post.toggleIsFixed();
        return PostDto.Response.of(post);
    }


    /**
     *  추후에 검사 기능 추가
     */
    private void checkFixable(ClubMember clubMember) {
        if (!clubMember.getRole().equals(ClubRole.ADMIN)) {
            throw new RuntimeException(); // 권한 x
        }
    }
}

