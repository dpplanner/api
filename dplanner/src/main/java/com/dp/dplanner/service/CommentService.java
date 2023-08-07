package com.dp.dplanner.service;


import com.dp.dplanner.domain.Comment;
import com.dp.dplanner.domain.CommentMemberLike;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.club.ClubRole;
import com.dp.dplanner.dto.CommentDto;
import com.dp.dplanner.dto.CommentMemberLikeDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.CommentMemberLikeRepository;
import com.dp.dplanner.repository.CommentRepository;
import com.dp.dplanner.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {


    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CommentMemberLikeRepository commentMemberLikeRepository;

    @Transactional
    public CommentDto.Response createComment(Long clubMemberId,CommentDto.Create createDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        Post post = postRepository.findById(createDto.getPostId()).orElseThrow(RuntimeException::new);
        Comment parent = null;

        if (createDto.getParentId() != null) {
            parent = commentRepository.findById(createDto.getParentId()).orElseThrow(RuntimeException::new);
            checkIsParent(parent, post.getId());
        }

        Comment savedComment = commentRepository.save(createDto.toEntity(clubMember, post, parent));

        if(parent != null)  parent.addChildren(savedComment);

        return CommentDto.Response.of(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentDto.Response> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findCommentsUsingPostId(postId);
        return CommentDto.Response.ofList(comments);
    }
    @Transactional(readOnly = true)
    public List<CommentDto.Response> getCommentsByClubMemberId(Long clubMemberId) {
        List<Comment> comments = commentRepository.findCommentsByClubMemberId(clubMemberId);
        return CommentDto.Response.ofList(comments);
    }

    private void checkIsParent(Comment parent,Long postId) {
        if ((parent.getParent() != null) || !(postId.equals(parent.getPost().getId()))) {
            throw new RuntimeException(); // 원본 댓글에만 대댓 가능 , 해당 댓글이 같은 post에서 작성된 것인지
        }
    }

    @Transactional
    public CommentDto.Response updateComment(Long clubMemberId, CommentDto.Update updateDto) {

        Comment comment = commentRepository.findById(updateDto.getId()).orElseThrow(RuntimeException::new);
        if (!checkAuthentication(clubMemberId, comment)) {
            throw new RuntimeException();
        }
        comment.update(updateDto.getContent());
        return CommentDto.Response.of(comment);
    }

    @Transactional
    public void deleteComment(Long clubMemberId, Long commentId) {

        Comment comment = commentRepository.findById(commentId).orElseThrow(RuntimeException::new);

        if (!(checkAuthentication(clubMemberId, comment) || checkAuthorization(clubMemberId))) {
            throw new RuntimeException();
        }
        commentRepository.delete(comment);

    }

    private boolean checkAuthentication(Long clubMemberId, Comment comment) {
        return comment.getClubMember().getId().equals(clubMemberId);
    }

    private boolean checkAuthorization(Long clubMemberId) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        return clubMember.getRole().equals(ClubRole.ADMIN);
    }

    @Transactional
    public CommentMemberLikeDto.Response likeComment(long clubMemberId, long commentId) {

        Optional<CommentMemberLike> find = commentMemberLikeRepository.findCommentMemberLikeByClubMemberIdAndCommentId(clubMemberId,commentId);

        if (find.isEmpty()) {

            Comment comment = commentRepository.findById(commentId).orElseThrow(RuntimeException::new);
            ClubMember clubMember =clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);

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

}
