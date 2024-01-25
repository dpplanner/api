package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.AttachmentDto;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.exception.ClubException;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.PostException;
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
    public Response createPost(Long clubMemberId, Create create) {
        ClubMember clubMember = getClubMember(clubMemberId);
        Club club = getClub(create.getClubId());
        checkIsSameClub(clubMemberId,club.getId());

        Post post = postRepository.save(create.toEntity(clubMember,club));
        if (create.getFiles() != null && create.getFiles().size() != 0) {
            attachmentService.createAttachment(
                    AttachmentDto.Create.builder()
                            .postId(post.getId())
                            .files(create.getFiles())
                            .build());
        }

        return Response.of(post,0,0);
    }

    public Response getPostById(Long clubMemberId, Long postId) {

        Post post = getPost(postId);
        checkIsSameClub(clubMemberId, post.getClub().getId());


        int likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());
        int commentCount = commentRepository.countDistinctByPostId(post.getId());

        return Response.of(post, likeCount, commentCount);
    }

    public SliceResponse getPostsByClubId(Long clubMemberId, Long clubId, Pageable pageable) {

        checkIsSameClub(clubMemberId, clubId);
        Slice<Post> postSlice = postRepository.findByClubId(clubId, pageable);

        return getSliceResponse(pageable, postSlice);

    }

    public SliceResponse getMyPostsByClubId(Long clubMemberId, Long clubId, Pageable pageable) {

        checkIsSameClub(clubMemberId, clubId);
        Slice<Post> postSlice = postRepository.findMyPostsByClubId(clubMemberId, clubId, pageable);

        return getSliceResponse(pageable, postSlice);

    }

    private SliceResponse getSliceResponse(Pageable pageable, Slice<Post> postSlice) {
        List<Integer> likeCounts = postSlice.getContent().stream().map(post -> postMemberLikeRepository.countDistinctByPostId(post.getId())).toList();
        // toDO 배치 단위로 구하기
        List<Integer> commentCounts = postSlice.getContent().stream().map(post -> commentRepository.countDistinctByPostId(post.getId())).toList();
        return new SliceResponse(Response.ofList(postSlice.getContent(), likeCounts, commentCounts), pageable, postSlice.hasNext());
    }

    @Transactional
    public Response updatePost(Long clubMemberId, Update update) {

        Post post = getPost(update.getId());
        checkUpdatable(post.getClubMember(), clubMemberId);

        List<String> deletedAttachmentUrl = post.getAttachments().stream()
                .map(Attachment::getUrl)
                .filter(url -> !update.getAttachmentUrl().contains(url))
                .toList();

        attachmentService.deleteAttachmentsByUrl(post, deletedAttachmentUrl);

        if (update.getFiles() != null && update.getFiles().size() != 0) {
            attachmentService.createAttachment(
                    AttachmentDto.Create.builder()
                            .postId(post.getId())
                            .files(update.getFiles())
                            .build());
        }


        post.updateContent(update.getContent());

        int likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());
        int commentCount = commentRepository.countDistinctByPostId(post.getId());

        return Response.of(post,likeCount,commentCount);
    }

    @Transactional
    public void deletePostById(Long clubMemberId, Long postId) {

        ClubMember clubMember = getClubMember(clubMemberId);
        Post post = getPost(postId);
        checkDeletable(clubMember, post.getClubMember().getId());

        postRepository.delete(post);

    }

    @Transactional
    public PostMemberLikeDto.Response likePost(Long clubMemberId,Long postId) {

        Optional<PostMemberLike> find = postMemberLikeRepository.findByClubMemberIdAndPostId(clubMemberId,postId);

        if (find.isEmpty()) {

            Post post = getPost(postId);
            ClubMember clubMember = getClubMember(clubMemberId);

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
    @RequiredAuthority(authority = POST_ALL)
    public Response toggleIsFixed(Long clubMemberId, Long postId) {

        Post post = getPost(postId);
        post.toggleIsFixed();

        int likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());
        int commentCount = commentRepository.countDistinctByPostId(post.getId());

        return Response.of(post,likeCount,commentCount);
    }

    private void checkIsSameClub(Long clubMemberId, Long clubId) {
        ClubMember clubMember = getClubMember(clubMemberId);
        if (!clubMember.isSameClub(clubId)) {
            throw new ClubMemberException(DIFFERENT_CLUB_EXCEPTION);
        }
    }

    private void checkDeletable(ClubMember clubMember, Long clubMemberId) {

        if (!clubMember.getId().equals(clubMemberId)) {
            if(!clubMemberService.hasAuthority(clubMember.getId(), POST_ALL)){
                throw new PostException(DELETE_AUTHORIZATION_DENIED);
            }
        }

    }
    private void checkUpdatable(ClubMember clubMember, Long clubMemberId) {

        if (!clubMember.getId().equals(clubMemberId)){
            throw new PostException(UPDATE_AUTHORIZATION_DENIED);
        }
    }
    private Post getPost(Long postId) {
        return postRepository.findById(postId).orElseThrow(() -> new PostException(POST_NOT_FOUND));
    }

    private Club getClub(Long clubId) {
        return clubRepository.findById(clubId).orElseThrow(() -> new ClubException(CLUB_NOT_FOUND));
    }

    private ClubMember getClubMember(Long clubMemberId) {
        return clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));
    }
}

