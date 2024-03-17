package com.dp.dplanner.adapter.dto;

import com.dp.dplanner.domain.CommentMemberLike;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class CommentMemberLikeDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    static public class Response {
        private long id;
        private long clubMemberId;
        private long postId;
        private Status status;

        public static CommentMemberLikeDto.Response like(CommentMemberLike commentMemberLike) {
            return CommentMemberLikeDto.Response.builder()
                    .id(commentMemberLike.getId())
                    .clubMemberId(commentMemberLike.getClubMember().getId())
                    .postId(commentMemberLike.getComment().getId())
                    .status(Status.LIKE)
                    .build();
        }

        public static CommentMemberLikeDto.Response dislike(CommentMemberLike commentMemberLike) {
            return CommentMemberLikeDto.Response.builder()
                    .id(commentMemberLike.getId())
                    .clubMemberId(commentMemberLike.getClubMember().getId())
                    .postId(commentMemberLike.getComment().getId())
                    .status(Status.DISLIKE)
                    .build();
        }
    }
}
