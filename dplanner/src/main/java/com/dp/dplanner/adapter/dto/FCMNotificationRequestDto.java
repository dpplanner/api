package com.dp.dplanner.adapter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class FCMNotificationRequestDto {

    private String refreshFcmToken;

}
