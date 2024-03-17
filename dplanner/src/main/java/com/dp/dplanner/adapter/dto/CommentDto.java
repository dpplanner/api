package com.dp.dplanner.adapter.dto;

import com.dp.dplanner.domain.Comment;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.ClubMember;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class CommentDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Create {
        @NotNull
        private Long postId;
        private Long parentId;
        private String content;

        public Comment toEntity(ClubMember clubMember, Post post, Comment parent) {
            return  Comment.builder()
                    .post(post)
                    .clubMember(clubMember)
                    .club(post.getClub())
                    .parent(parent)
                    .content(content)
                    .build();
        }

    }
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommentResponseDto{
        private Comment comment;
        private Boolean likeStatus;
        private Long likeCount;

    }
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private Long id;
        private Long parentId;
        private Long postId;
        private Long clubMemberId;
        private String clubMemberName;
        private String profileUrl;
        private Long likeCount;
        private String content;
        private Boolean isDeleted;
        private Boolean likeStatus;
        private List<Response> children;
        private LocalDateTime createdTime;
        private LocalDateTime lastModifiedTime;

        public static Response of(Comment comment,Long likeCount,Boolean likeStatus) {

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
                    .profileUrl(comment.getClubMember().getUrl())
                    .postId(comment.getPost().getId())
                    .children(children)
                    .createdTime(comment.getCreatedDate())
                    .lastModifiedTime(comment.getLastModifiedDate())
                    .clubMemberName(comment.getClubMember().getName())
                    .likeCount(likeCount)
                    .isDeleted(comment.getIsDeleted())
                    .likeStatus(likeStatus)
                    .build();

        }

        public static List<Response> ofList(List<CommentResponseDto> commentResponseDtos) {
            List<Response> commentResponseList = new ArrayList<>();
            Map<Long, Response> responseMap = new HashMap<>();

            IntStream.range(0,commentResponseDtos.size()).forEach(
                    index -> {
                        Response response = Response.of(commentResponseDtos.get(index).getComment(), commentResponseDtos.get(index).getLikeCount(), commentResponseDtos.get(index).likeStatus);
                        responseMap.put(response.getId(), response);
                        if (commentResponseDtos.get(index).getComment().getParent() != null) {
                            try {
                                responseMap.get(commentResponseDtos.get(index).getComment().getParent().getId()).getChildren().add(response);
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
    @NoArgsConstructor
    public static class Update {
        private Long id;
        private String content;

    }
}
