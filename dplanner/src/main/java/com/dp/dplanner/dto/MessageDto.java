package com.dp.dplanner.dto;


import com.dp.dplanner.domain.message.PrivateMessage;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

public class MessageDto {


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response{
        private Long id;
        private String content;
        private String title;
        private String redirectUrl;
        private Boolean isRead;
        public static Response of(PrivateMessage message) {

            return Response.builder()
                    .id(message.getId())
                    .title(message.getTitle())
                    .content(message.getContent())
                    .redirectUrl(message.getRedirectUrl())
                    .isRead(message.getIsRead())
                    .build();
        }

    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResponseList{
        private List<Response> responseList = new ArrayList<>();
        private Long notRead;


    }
}
