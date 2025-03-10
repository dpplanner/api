package com.dp.dplanner.adapter.dto;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubMember;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

public class ClubMemberDto {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        private Long id;

        public static List<ClubMemberDto.Request> ofList(List<Long> clubMembersIds) {
            return clubMembersIds.stream().map(ClubMemberDto.Request::new).toList();
        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
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
    @NoArgsConstructor
    public static class Update {
        private Long id;
        private Long clubAuthorityId;
        private String name;
        private String info;
        private String role;

    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
            private Long id;
            private String name;
            private String info;
            private String role;
            private Boolean isConfirmed;
            private String url;
            private Long clubAuthorityId;
            private String clubAuthorityName;
            @Builder.Default
            private List<String> clubAuthorityTypes = new ArrayList<>();

        public static ClubMemberDto.Response of(ClubMember clubMember) {
            return Response.builder()
                    .id(clubMember.getId())
                    .name(clubMember.getName())
                    .info(clubMember.getInfo())
                    .role(clubMember.getRole().name())
                    .isConfirmed(clubMember.getIsConfirmed())
                    .url(clubMember.getUrl())
                    .clubAuthorityId(clubMember.getClubAuthority() != null ? clubMember.getClubAuthority().getId() : null)
                    .clubAuthorityName(clubMember.getClubAuthority() != null ? clubMember.getClubAuthority().getName() : null)
                    .clubAuthorityTypes(clubMember.getClubAuthority() != null ? clubMember.getClubAuthority().getClubAuthorityTypes().stream().map(ClubAuthorityType::toString).toList() : null)
                    .build();
        }

        public static List<ClubMemberDto.Response> ofList(List<ClubMember> clubMembers) {
            return clubMembers.stream().map(ClubMemberDto.Response::of).toList();
        }
    }

    public interface ResponseMapping{
        Long getId();
        Long getClubId();
        String getName();
        String getInfo();
        String getRole();
        Integer getIsConfirmed();
        String getUrl();
        Long getClubAuthorityId();
        String getClubAuthorityName();
        List<String> getClubAuthorityTypes();
    }

}
