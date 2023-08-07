package com.dp.dplanner.dto;

import com.dp.dplanner.domain.PostMemberLike;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class PostMemberLikeDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    static public class Response {

        private long id;
        private long clubMemberId;
        private long postId;
        private Status status;


        public static Response like(PostMemberLike postMemberLike) {
            return Response.builder()
                    .id(postMemberLike.getId())
                    .clubMemberId(postMemberLike.getClubMember().getId())
                    .postId(postMemberLike.getPost().getId())
                    .status(Status.LIKE)
                    .build();
        }

        public static Response dislike(PostMemberLike postMemberLike) {

            return   Response.builder()
                    .id(postMemberLike.getId())
                    .clubMemberId(postMemberLike.getClubMember().getId())
                    .postId(postMemberLike.getPost().getId())
                    .status(Status.DISLIKE)
                    .build();
        }
    }
}
