package com.dp.dplanner.dto;

import com.dp.dplanner.domain.club.ClubMember;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ClubMemberDto {

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Response {
            private Long id;
            private String name;
            private String info;
            private String role;

        public static ClubMemberDto.Response of(ClubMember clubMember) {
            return new ClubMemberDto.Response(
                    clubMember.getId(),
                    clubMember.getMember().getName(),
                    clubMember.getMember().getInfo(),
                    clubMember.getRole().name()
            );
        }
    }
}
