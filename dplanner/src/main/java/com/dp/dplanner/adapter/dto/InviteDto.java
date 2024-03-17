package com.dp.dplanner.adapter.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InviteDto {

    private Long clubId;
    private String inviteCode;
    private Boolean verify;
}
