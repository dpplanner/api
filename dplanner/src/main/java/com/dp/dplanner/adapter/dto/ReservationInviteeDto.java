package com.dp.dplanner.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationInviteeDto {

    private Long clubMemberId;
    private String clubMemberName;
    private String profileImageUrl;
    private Boolean clubMemberIsDeleted;
}
