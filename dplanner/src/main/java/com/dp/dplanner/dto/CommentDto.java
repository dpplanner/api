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
import java.util.stream.Collectors;

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
                    .content(this.getContent())
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
            List<Response> children;
            if (comment.getParent() != null) {
                parentId = comment.getParent().getId();
                children = new ArrayList<>();
            }else{
                children = comment.getChildren().stream().map(Response::of).collect(Collectors.toList());
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
                        if (comment.getParent() != null) responseMap.get(comment.getParent().getId()).getChildren().add(response);
                        else commentResponseList.add(response);
                    }
            );

            return commentResponseList;
        }
    }


}
