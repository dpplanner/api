package com.dp.dplanner.service;


import com.dp.dplanner.domain.Comment;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.CommentDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.CommentRepository;
import com.dp.dplanner.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {


    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ClubMemberRepository clubMemberRepository;

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

    private void checkIsParent(Comment parent,Long postId) {
        if ((parent.getParent() != null) || !(postId.equals(parent.getPost().getId()))) {
            throw new RuntimeException(); // 원본 댓글에만 대댓 가능 , 해당 댓글이 같은 post에서 작성된 것인지
        }
    }

    public List<CommentDto.Response> getCommentsByPostId(Long postId) {

        List<Comment> comments = commentRepository.findCommentsUsingPostId(postId);
        return CommentDto.Response.ofList(comments);
    }

}
