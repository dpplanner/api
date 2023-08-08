package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Comment;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.ClubMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Create {
        private Long postId;
        private Long parentId;
        private String content;

        public Comment toEntity(ClubMember clubMember, Post post, Comment parent) {
            return  Comment.builder()
                    .post(post)
                    .clubMember(clubMember)
                    .parent(parent)
                    .content(content)
                    .build();
        }

    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long parentId;
        private Long postId;
        private Long clubMemberId;
        private String content;
        private List<Response> children;

        public static Response of(Comment comment) {

            Long parentId = null;
            List<Response> children = new ArrayList<>();
            if (comment.getParent() != null) {
                parentId = comment.getParent().getId();
            }
            return Response.builder()
                    .id(comment.getId())
                    .content(comment.getContent())
                    .parentId(parentId)
                    .clubMemberId(comment.getClubMember().getId())
                    .postId(comment.getPost().getId())
                    .children(children)
                    .build();

        }

        public static List<Response> ofList(List<Comment> comments) {

            List<Response> commentResponseList = new ArrayList<>();
            Map<Long, Response> responseMap = new HashMap<>();
            comments.forEach(comment -> {
                        Response response = Response.of(comment);
                        responseMap.put(response.getId(), response);
                        if (comment.getParent() != null) {
                            try {
                                responseMap.get(comment.getParent().getId()).getChildren().add(response);
                            } catch (NullPointerException e) {
                                commentResponseList.add(response); // CommentService :: getCommentsByClubMemberId 할 때 다른 사람이 작성한 원본 댓글에 대댓글 달았을 때 발생하는 오류
                            }
                        }
                        else commentResponseList.add(response);
                    }
            );

            return commentResponseList;
        }

    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Update {
        private Long id;
        private String content;

    }
}
