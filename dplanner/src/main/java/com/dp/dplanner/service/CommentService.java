package com.dp.dplanner.service;


import com.dp.dplanner.adapter.dto.CommentDto;
import com.dp.dplanner.domain.*;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.adapter.dto.CommentMemberLikeDto;
import com.dp.dplanner.repository.*;
import com.dp.dplanner.service.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.domain.club.ClubAuthorityType.POST_ALL;
import static com.dp.dplanner.adapter.dto.CommentDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CommentMemberLikeRepository commentMemberLikeRepository;
    private final CommentReportRepository commentReportRepository;

    private final MessageService messageService;

    @Transactional
    public Response createComment(Long clubMemberId, Create createDto) {

        ClubMember clubMember = getClubMember(clubMemberId);
        Post post = getPost(createDto.getPostId());
        checkIsSameClub(clubMember,post.getClub().getId());

        Comment parent = null;
        if (createDto.getParentId() != null) {
            parent = getComment(createDto.getParentId());
            checkIsParent(parent, post.getId());
        }
        Comment savedComment = commentRepository.save(createDto.toEntity(clubMember, post, parent));

        messageService.createPrivateMessage(List.of(post.getClubMember()),
                Message.commentMessage(
                        Message.MessageContentBuildDto.builder().
                                clubMemberName(savedComment.getClubMember().getName()).
                                postTitle(post.getTitle()).
                                info(String.valueOf(post.getId())).
                                build()));

        return Response.of(savedComment,0L,false);
    }

    public List<Response> getCommentsByPostId(Long clubMemberId, Long postId) {

        Post post = getPost(postId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, post.getClub().getId());

        List<Object[]> results = commentRepository.findCommentsUsingPostId(postId,clubMemberId);
        return getResponseList(results);
    }

    @Transactional
    public Response updateComment(Long clubMemberId, Update updateDto) {

        Comment comment = getComment(updateDto.getId());
        checkIsUpdatable(clubMemberId, comment);
        comment.update(updateDto.getContent());
        Long likeCount = commentMemberLikeRepository.countDistinctByCommentId(comment.getId());

        return Response.of(comment,likeCount,null);
    }

    @Transactional
    public void deleteComment(Long clubMemberId, Long commentId) {

        Comment comment = getComment(commentId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsDeletable(comment.getClubMember().getId(), clubMember);
        if (comment.getChildren().size() != 0) {
            comment.delete(); // isDeleted = true
        }else{
            commentRepository.delete(getDeletableAncestorComment(comment));
        }
    }

    private Comment getDeletableAncestorComment(Comment comment) {
        Comment parent = comment.getParent(); // 현재 댓글의 부모를 구함
        if (parent != null && parent.getChildren().size() == 1 && parent.getIsDeleted()) {
            // 부모가 있고, 부모의 자식이 1개(지금 삭제하는 댓글)이고, 부모의 삭제 상태가 TRUE인 댓글이라면 재귀
            return getDeletableAncestorComment(parent);
        }
        return comment; // 삭제해야하는 댓글 반환
    }

    @Transactional
    public CommentMemberLikeDto.Response likeComment(Long clubMemberId, Long commentId) {

        Comment comment = getComment(commentId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember,comment.getClub().getId());

        Optional<CommentMemberLike> find = commentMemberLikeRepository.findCommentMemberLikeByClubMemberIdAndCommentId(clubMemberId, commentId);

        if (find.isEmpty()) {
            CommentMemberLike commentMemberLike = commentMemberLikeRepository.save(
                    CommentMemberLike.builder()
                            .clubMember(clubMember)
                            .comment(comment)
                            .build()
            );
            return CommentMemberLikeDto.Response.like(commentMemberLike);
        }else{
            CommentMemberLike clubMemberLike = find.get();
            commentMemberLikeRepository.delete(clubMemberLike);
            return CommentMemberLikeDto.Response.dislike(clubMemberLike);
        }

    }



    /**
     * utility methods
     */
    private static void checkIsSameClub(ClubMember clubMember,Long targetClubId) {
        if (!clubMember.isSameClub(targetClubId)) {
            throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
        }
    }
    private static void checkIsParent(Comment parent,Long postId) {
        if ((parent.getParent() != null) || !(postId.equals(parent.getPost().getId()))) {
            throw new ServiceException(CREATE_COMMENT_DENIED); // 원본 댓글에만 대댓 가능 , 해당 댓글이 같은 post에서 작성된 것인지
        }
    }
    private static void checkIsUpdatable(Long clubMemberId, Comment comment) {
        if (!comment.getClubMember().getId().equals(clubMemberId)) {
            throw new ServiceException(UPDATE_AUTHORIZATION_DENIED);
        }
    }
    private void checkIsDeletable(Long writerClubMemberId, ClubMember clubMember) {
        if (!clubMember.getId().equals(writerClubMemberId)) {
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
    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new ServiceException(COMMENT_NOT_FOUND));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId).orElseThrow(()->new ServiceException(POST_NOT_FOUND));
    }

    private ClubMember getClubMember(Long clubMemberId) {
        return clubMemberRepository.findById(clubMemberId).orElseThrow(()->new ServiceException(CLUBMEMBER_NOT_FOUND));
    }

    private List<Response> getResponseList(List<Object[]> results) {
        List<CommentResponseDto> commentResponseDtos = new ArrayList<>();
        for (Object[] result : results) {
            commentResponseDtos.add(
                    CommentResponseDto.builder()
                            .comment((Comment) result[0])
                            .likeStatus(result[1] != null)
                            .likeCount((Long) result[2])
                            .build()
            );
        }
        return Response.ofList(commentResponseDtos);
    }

    @Transactional
    public void reportComment(CommentDto.Report report) {
        Comment comment = getComment(report.getCommentId());
        ClubMember clubMember = getClubMember(report.getClubMemberId());
        checkIsSameClub(clubMember, comment.getClub().getId());

        CommentReport commentReport = CommentReport.builder()
                .comment(comment)
                .clubMember(clubMember)
                .message(report.getReportMessage()).build();

        commentReportRepository.save(commentReport);
        messageService.createPrivateMessage(
                List.of(comment.getClubMember()),
                Message.commentReportedMessage(
                        Message.MessageContentBuildDto.builder()
                                .info(comment.getPost().getId().toString())
                                .build())
        );

        List<ClubMember> managers = clubMemberRepository.findClubMemberByClubIdAndClubAuthorityTypesContaining(comment.getClub().getId(), POST_ALL);
        messageService.createPrivateMessage(
                managers,
                Message.commentReportedADMINMessage(
                        Message.MessageContentBuildDto.builder()
                                .info(comment.getPost().getId().toString())
                                .build())
        );
    }

}
