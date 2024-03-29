package com.dp.dplanner.adapter.dto;

import lombok.*;

import java.util.List;


public class FCMDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor
    public static class Request{
        private String refreshFcmToken;
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor
    public static class Send{

        private String title;
        private String content;

        private List<String> fcmTokens;

    }

}
