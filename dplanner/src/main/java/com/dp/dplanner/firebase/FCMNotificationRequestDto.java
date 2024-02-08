package com.dp.dplanner.firebase;

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
