package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.club.ClubRole;
import com.dp.dplanner.dto.PostDto;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.repository.PostMemberLikeRepository;
import com.dp.dplanner.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// ToDo : ClubId 추가 , MemberId 추가

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostMemberLikeRepository postMemberLikeRepository;

    private final ClubMemberRepository clubMemberRepository;

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
    public PostDto.Response createPost(PostDto.Create create) {

        Post post = postRepository.save(create.toEntity());

        return PostDto.Response.of(post);
    }


    @Transactional
    public PostDto.Response updatePost(PostDto.Update update, long memberId) {

        Post post = postRepository.findById(update.getId()).orElseThrow(RuntimeException::new);

        if (!post.getMember().getId().equals(memberId)){
            throw new RuntimeException();
        }

        post.update(update);
        return PostDto.Response.of(post);
    }


    @Transactional
    public void deletePostById(long clubId, long memberId, long postId) {

        Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);
        ClubMember clubMember = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId).orElseThrow(RuntimeException::new);
        checkDeletable(memberId, clubMember);
        postRepository.delete(post);

    }

    /**
     *  추후에 검사 기능 추가
     */
    private void checkDeletable(long memberId, ClubMember clubMember) {

        if (! (clubMember.getMember().getId().equals(memberId) || clubMember.getRole().equals(ClubRole.ADMIN))){
            throw new RuntimeException(); // 권한 X
        }

    }


    @Transactional
    public PostMemberLikeDto.Response likePost(long postId, long memberId) {

        Optional<PostMemberLike> find = postMemberLikeRepository.findPostMemberLikeByMemberIdAndPostId(postId, memberId);

        if (find.isEmpty()) {

            Post post = postRepository.findById(postId).orElseThrow(RuntimeException::new);
            Member member = memberRepository.findById(memberId).orElseThrow(RuntimeException::new);

            PostMemberLike postMemberLike = postMemberLikeRepository.save(
                    PostMemberLike.builder()
                            .member(member)
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
    public PostDto.Response toggleIsFixed(long clubId, long memberId, long postId) {

        ClubMember clubMember = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId).orElseThrow(RuntimeException::new);
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

