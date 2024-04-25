package com.dp.dplanner.adapter.bo;

import com.dp.dplanner.domain.club.ClubAuthority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubMemberBO {
    private Long id;
    private Long clubId;
    private String name;
    private String info;
    private String role;
    private Boolean isConfirmed;
    private String url;

    private ClubBO club;
    private MemberBO member;
    private ClubAuthorityBO clubAuthority;



}
