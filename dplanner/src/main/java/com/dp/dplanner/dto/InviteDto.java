package com.dp.dplanner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InviteDto {

    private Long clubId;
    private String inviteCode;

}
