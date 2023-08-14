package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.AttachmentDto;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.exception.club.ClubException;
import com.dp.dplanner.exception.clubMember.ClubMemberException;
import com.dp.dplanner.exception.post.PostException;
import com.dp.dplanner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.dto.PostDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostMemberLikeRepository postMemberLikeRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberService clubMemberService;
    private final AttachmentService attachmentService;


    @Transactional
    public Response createPost(long clubMemberId, Create create) {
        ClubMember clubMember = getClubMember(clubMemberId);
        Club club = getClub(create.getClubId());
        checkIsSameClub(clubMember,club.getId());

        Post post = postRepository.save(create.toEntity(clubMember,club));

        attachmentService.createAttachment(
                AttachmentDto.Create.builder()
                        .postId(post.getId())
                        .files(create.getFiles())
                        .build());

        return Response.of(post,0,0);
    }

    public Response getPostById(long clubMemberId, long postId) {

        ClubMember clubMember = getClubMember(clubMemberId);
        Post post = getPost(postId);
        checkIsSameClub(clubMember, post.getClub().getId());


        int likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());
        int commentCount = commentRepository.countDistinctByPostId(post.getId());
        return Response.of(post, likeCount, commentCount);
    }

    public SliceResponse getPostsByClubId(long clubMemberId, long clubId, Pageable pageable) {

        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, clubId);
        Slice<Post> postSlice = postRepository.findByClubId(clubId, pageable);

        List<Integer> likeCounts = postSlice.getContent().stream().map(post -> postMemberLikeRepository.countDistinctByPostId(post.getId())).toList();
        List<Integer> commentCounts = postSlice.getContent().stream().map(post -> commentRepository.countDistinctByPostId(post.getId())).toList();

        return new SliceResponse(Response.ofList(postSlice.getContent(), likeCounts, commentCounts), pageable, postSlice.hasNext());

    }

    @Transactional
    public Response updatePost(long clubMemberId, Update update) {

        Post post = getPost(update.getId());
        checkUpdatable(post.getClubMember(), clubMemberId);
        post.update(update);

        int likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());
        int commentCount = commentRepository.countDistinctByPostId(post.getId());

        return Response.of(post,likeCount,commentCount);
    }

    @Transactional
    public void deletePostById(long clubMemberId, long postId) {

        ClubMember clubMember = getClubMember(clubMemberId);
        Post post = getPost(postId);
        checkDeletable(clubMember, post.getClubMember().getId());

        postRepository.delete(post);

    }

    @Transactional
    public PostMemberLikeDto.Response likePost(long clubMemberId,long postId) {

        Optional<PostMemberLike> find = postMemberLikeRepository.findByClubMemberIdAndPostId(clubMemberId,postId);

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

        Post post = getPost(postId);
        post.toggleIsFixed();

        int likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());
        int commentCount = commentRepository.countDistinctByPostId(post.getId());
        return Response.of(post,likeCount,commentCount);
    }

    private void checkIsSameClub(ClubMember clubMember, long clubId) {
        if (!clubMember.isSameClub(clubId)) {
            throw new RuntimeException();
        }
    }

    private void checkDeletable(ClubMember clubMember, long clubMemberId) {

        if (!clubMember.getId().equals(clubMemberId)) {
            if(!clubMemberService.hasAuthority(clubMember.getId(), POST_ALL)){
                throw new PostException(DELETE_AUTHORIZATION_DENIED);
            }
        }

    }

    private void checkUpdatable(ClubMember clubMember, long clubMemberId) {

        if (!clubMember.getId().equals(clubMemberId)){
            throw new PostException(UPDATE_AUTHORIZATION_DENIED);
        }
    }
    private Post getPost(long postId) {
        return postRepository.findById(postId).orElseThrow(() -> new PostException(POST_NOT_FOUND));
    }
    private Club getClub(long clubId) {
        return clubRepository.findById(clubId).orElseThrow(() -> new ClubException(CLUB_NOT_FOUND));
    }

    private ClubMember getClubMember(long clubMemberId) {
        return clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));
    }
}

