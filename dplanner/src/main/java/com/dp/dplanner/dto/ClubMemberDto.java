package com.dp.dplanner.dto;

import com.dp.dplanner.domain.club.ClubMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ClubMemberDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Update {
        private Long id;
        private String name;
        private String info;
        private String role;

    }

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
                    clubMember.getName(),
                    clubMember.getInfo(),
                    clubMember.getRole().name()
            );
        }

        public static List<ClubMemberDto.Response> ofList(List<ClubMember> clubMembers) {
            return clubMembers.stream().map(ClubMemberDto.Response::of).toList();
        }
    }
}
