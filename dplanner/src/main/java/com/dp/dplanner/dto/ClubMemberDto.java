package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
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
    @AllArgsConstructor
    public static class Request {
        private Long id;

        public static List<ClubMemberDto.Request> ofList(List<Long> clubMembersIds) {
            return clubMembersIds.stream().map(Request::new).toList();
        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Create {
        private Long clubId;
        private String name;
        private String info;

        public ClubMember toEntity(Member member, Club club) {
            return ClubMember.builder()
                    .member(member)
                    .club(club)
                    .name(name)
                    .info(info)
                    .build();
        }

    }

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
    public static class Delete {
        private Long id;

        public static List<ClubMemberDto.Delete> ofList(List<Long> clubMembersIds) {
            return clubMembersIds.stream().map(Delete::new).toList();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Response {
            private Long id;
            private String name;
            private String info;
            private String role;
            private boolean isConfirmed;

        public static ClubMemberDto.Response of(ClubMember clubMember) {
            return new ClubMemberDto.Response(
                    clubMember.getId(),
                    clubMember.getName(),
                    clubMember.getInfo(),
                    clubMember.getRole().name(),
                    clubMember.isConfirmed()
            );
        }

        public static List<ClubMemberDto.Response> ofList(List<ClubMember> clubMembers) {
            return clubMembers.stream().map(ClubMemberDto.Response::of).toList();
        }
    }
}
