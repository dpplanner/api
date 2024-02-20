package com.dp.dplanner.service;


import com.dp.dplanner.domain.Comment;
import com.dp.dplanner.domain.CommentMemberLike;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.dto.CommentMemberLikeDto;
import com.dp.dplanner.exception.*;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.CommentMemberLikeRepository;
import com.dp.dplanner.repository.CommentRepository;
import com.dp.dplanner.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.domain.club.ClubAuthorityType.POST_ALL;
import static com.dp.dplanner.dto.CommentDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CommentMemberLikeRepository commentMemberLikeRepository;
    private final ClubMemberService clubMemberService;

    private final MessageService messageService;

    @Transactional
    public Response createComment(Long clubMemberId, Create createDto) {

        ClubMember clubMember = getClubMember(clubMemberId);
        Post post = getPost(createDto.getPostId());
        checkIsSameClub(post.getClub().getId(),clubMember);

        Comment parent = null;

        if (createDto.getParentId() != null) {
            parent = getComment(createDto.getParentId());
            checkIsParent(parent, post.getId());
        }
        Comment savedComment = commentRepository.save(createDto.toEntity(clubMember, post, parent));

        messageService.createPrivateMessage(List.of(post.getClubMember().getId()), Message.commentMessage());

        return Response.of(savedComment,0);
    }

    public List<Response> getCommentsByPostId(Long clubMemberId, Long postId) {

        Post post = getPost(postId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(post.getClub().getId(), clubMember);

        List<Comment> comments = commentRepository.findCommentsUsingPostId(postId);
        List<Integer> likeCounts = getLikeCounts(comments);

        return Response.ofList(comments,likeCounts);
    }

    public List<Response> getCommentsByClubMemberId(Long clubMemberId, Long clubId) {

        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubId, clubMember);

        List<Comment> comments = commentRepository.findCommentsByClubMemberId(clubMemberId);
        List<Integer> likeCounts = getLikeCounts(comments);

        return Response.ofList(comments, likeCounts);
    }

    @Transactional
    public Response updateComment(Long clubMemberId, Update updateDto) {

        Comment comment = getComment(updateDto.getId());
        checkUpdatable(clubMemberId, comment);
        comment.update(updateDto.getContent());

        int likeCount = commentMemberLikeRepository.countDistinctByCommentId(comment.getId());

        return Response.of(comment,likeCount);
    }

    @Transactional
    public void deleteComment(Long clubMemberId, Long commentId) {

        Comment comment = getComment(commentId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkDeletable(comment.getClubMember().getId(), clubMember);
        commentRepository.delete(comment);

    }

    @Transactional
    public CommentMemberLikeDto.Response likeComment(Long clubMemberId, Long commentId) {

        Comment comment = getComment(commentId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(comment.getClub().getId(), clubMember);

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


    private static void checkIsSameClub(Long clubId, ClubMember clubMember) {
        if (!clubMember.isSameClub(clubId)) {
            throw new CommentException(DIFFERENT_CLUB_EXCEPTION);
        }
    }

    private void checkIsParent(Comment parent,Long postId) {
        if ((parent.getParent() != null) || !(postId.equals(parent.getPost().getId()))) {
            throw new CommentException(CREATE_COMMENT_DENIED); // 원본 댓글에만 대댓 가능 , 해당 댓글이 같은 post에서 작성된 것인지
        }
    }

    private void checkDeletable(Long clubMemberId, ClubMember clubMember) {

        if (!clubMember.getId().equals(clubMemberId)) {
            if(!clubMemberService.hasAuthority(clubMember.getId(), POST_ALL)){
                throw new CommentException(DELETE_AUTHORIZATION_DENIED);
            }
        }

    }

    private void checkUpdatable(Long clubMemberId, Comment comment) {
        if (!comment.getClubMember().getId().equals(clubMemberId)) {
            throw new CommentException(UPDATE_AUTHORIZATION_DENIED);
        }
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new CommentException(COMMENT_NOT_FOUND));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId).orElseThrow(()->new PostException(POST_NOT_FOUND));
    }

    private ClubMember getClubMember(Long clubMemberId) {
        return clubMemberRepository.findById(clubMemberId).orElseThrow(()->new ClubMemberException(CLUBMEMBER_NOT_FOUND));
    }

    private List<Integer> getLikeCounts(List<Comment> comments) {
        return comments.stream().map(comment -> commentMemberLikeRepository.countDistinctByCommentId(comment.getId())).toList();
    }

}
