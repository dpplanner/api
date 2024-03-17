package com.dp.dplanner.adapter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class FCMNotificationRequestDto {

    private Long memberId;
    private String title;
    private String body;
    private String refreshFcmToken;

}
