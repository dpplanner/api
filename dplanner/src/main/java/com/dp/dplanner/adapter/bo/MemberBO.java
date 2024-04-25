package com.dp.dplanner.adapter.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberBO {
    private Long id;
    private String email;
    private String name;
    private String refreshToken;
    private String fcmToken;
    private ClubBO recentClub;

}
