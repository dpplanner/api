package com.dp.dplanner.service;

import com.dp.dplanner.adapter.dto.PostDto;
import com.dp.dplanner.domain.PostReport;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.service.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.PostMemberLike;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.adapter.dto.AttachmentDto;
import com.dp.dplanner.adapter.dto.PostMemberLikeDto;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.adapter.dto.PostDto.*;
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
    private final PostReportRepository postReportRepository;
    private final AttachmentService attachmentService;
    private final MessageService messageService;

    @Transactional
    public Response createPost(Long clubMemberId, Create create) {
        ClubMember clubMember = getClubMember(clubMemberId);
        Club club = getClub(create.getClubId());
        checkIsSameClub(clubMember,club.getId());

        Post post = postRepository.save(create.toEntity(clubMember,club));
        if (create.getFiles() != null && create.getFiles().size() != 0) {
            attachmentService.createAttachment(
                    AttachmentDto.Create.builder()
                            .postId(post.getId())
                            .files(create.getFiles())
                            .build());
        }
        return Response.of(post,0L,0L,false);
    }

    public Response getPostById(Long clubMemberId, Long postId) {
        Post post = getPost(postId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, post.getClub().getId());

        Long likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());
        Long commentCount = commentRepository.countDistinctByPostId(post.getId());
        Boolean likeStatus = postMemberLikeRepository.existsPostMemberLikeByPostIdAndClubMemberId(post.getId(), clubMemberId);
        return Response.of(post, likeCount, commentCount,likeStatus);
    }

    public SliceResponse getPostsByClubId(Long clubMemberId, Long clubId, Pageable pageable) {
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, clubId);
        Slice<Object[]> postSlice = postRepository.findByClubId(clubId, clubMemberId, pageable);
        return getSliceResponse(pageable, postSlice);
    }

    public SliceResponse getMyPostsByClubId(Long clubMemberId, Long clubId, Pageable pageable) {
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, clubId);
        Slice<Object[]> postSlice = postRepository.findMyPostsByClubId(clubMemberId, clubId, pageable);
        return getSliceResponse(pageable, postSlice);
    }

    public SliceResponse getLikePosts(Long clubMemberId, Long clubId, Pageable pageable) {
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, clubId);
        Slice<Object[]> postSlice = postRepository.findLikePosts(clubMemberId, clubId, pageable);
        return getSliceResponse(pageable, postSlice);
    }

    public PostDto.SliceResponse getCommentedPosts(Long clubMemberId, Long clubId, Pageable pageable) {
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember,clubId);
        Slice<Object[]> commentedPosts = postRepository.findMyCommentedPosts(clubMemberId, pageable);
        return PostDto.getSliceResponse(pageable,commentedPosts);
    }

    @Transactional
    public Response updatePost(Long clubMemberId, Update update) {
        Post post = getPost(update.getId());
        checkIsUpdatable(post.getClubMember(), clubMemberId);

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

        post.updatePost(update.getTitle(), update.getContent());

        Long likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());
        Long commentCount = commentRepository.countDistinctByPostId(post.getId());
        Boolean likeStatus = postMemberLikeRepository.existsPostMemberLikeByPostIdAndClubMemberId(post.getId(), clubMemberId);
        return Response.of(post,likeCount,commentCount,likeStatus);
    }

    @Transactional
    public void deletePostById(Long clubMemberId, Long postId) {
        ClubMember clubMember = getClubMember(clubMemberId);
        Post post = getPost(postId);
        checkIsDeletable(clubMember, post.getClubMember().getId());
        if (!clubMember.equals(post.getClubMember())) {
            messageService.createPrivateMessage(List.of(post.getClubMember()),
                    Message.postDeletedMessage(
                            Message.MessageContentBuildDto.builder().
                                    postTitle(post.getTitle()).
                                    info(String.valueOf(postId)).
                                    build()));
        }
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

        Long likeCount = postMemberLikeRepository.countDistinctByPostId(post.getId());
        Long commentCount = commentRepository.countDistinctByPostId(post.getId());
        Boolean likeStatus = postMemberLikeRepository.existsPostMemberLikeByPostIdAndClubMemberId(post.getId(), clubMemberId);
        return Response.of(post,likeCount,commentCount,likeStatus);
    }

    @Transactional
    public void reportPost(Report report) {
        Post post = getPost(report.getPostId());
        ClubMember clubMember = getClubMember(report.getClubMemberId());
        checkIsSameClub(clubMember, post.getClub().getId());

        PostReport postReport = PostReport.builder()
                .post(post)
                .clubMember(clubMember)
                .message(report.getReportMessage()).build();

        postReportRepository.save(postReport);
        messageService.createPrivateMessage(
                List.of(post.getClubMember()),
                Message.postReportedMessage(
                        Message.MessageContentBuildDto.builder()
                                .postTitle(post.getTitle())
                                .info(post.getId().toString())
                                .build())
        );
    }



    /**
     * utility methods
     */
    private void checkIsSameClub(ClubMember clubMember, Long targetClubId) {
        if (!clubMember.isSameClub(targetClubId)) {
            throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
        }
    }
    private void checkIsDeletable(ClubMember clubMember, Long clubMemberId) {
        if (!clubMember.getId().equals(clubMemberId)) {
            if(!hasAuthority(clubMember.getId(), POST_ALL)){
                throw new ServiceException(DELETE_AUTHORIZATION_DENIED);
            }
        }
    }
    private boolean hasAuthority(Long clubMemberId, ClubAuthorityType authority) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        return clubMember.hasAuthority(authority);
    }
    private void checkIsUpdatable(ClubMember clubMember, Long clubMemberId) {
        if (!clubMember.getId().equals(clubMemberId)){
            throw new ServiceException(UPDATE_AUTHORIZATION_DENIED);
        }
    }
    private Post getPost(Long postId) {
        return postRepository.findById(postId).orElseThrow(() -> new ServiceException(POST_NOT_FOUND));
    }
    private Club getClub(Long clubId) {
        return clubRepository.findById(clubId).orElseThrow(() -> new ServiceException(CLUB_NOT_FOUND));
    }
    private ClubMember getClubMember(Long clubMemberId) {
        return clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
    }
}

