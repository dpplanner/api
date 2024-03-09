package com.dp.dplanner.dto;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class ClubAuthorityDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Request {
        private Long clubId;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private Long clubId;
        private String name;
        private String description;
        @NotNull
        private List<String> authorities;

        public Create(Long clubId, String name, String description, String... authorities) {
            this.clubId = clubId;
            this.name = name;
            this.description = description;
            this.authorities = List.of(authorities);
        }

        public ClubAuthority toEntity(Club club) {
            return ClubAuthority.builder()
                    .club(club)
                    .clubAuthorityTypes(toClubAuthorityTypeList())
                    .name(name)
                    .description(description)
                    .build();
        }

        private List<ClubAuthorityType> toClubAuthorityTypeList() {
            return authorities.stream().map(ClubAuthorityType::valueOf).toList();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        @NotNull
        private Long id;
        private Long clubId;
        private String name;
        private String description;
        private List<String> authorities;

        public Update(Long id, Long clubId, String name, String description, String... authorities) {
            this.id = id;
            this.clubId = clubId;
            this.name = name;
            this.description = description;
            this.authorities = List.of(authorities);
        }

        public List<ClubAuthorityType> toClubAuthorityTypeList() {
            return authorities.stream().map(ClubAuthorityType::valueOf).toList();
        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Delete{
        private Long id;
        private Long clubId;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long clubId;
        private String name;
        private String description;
        private List<String> authorities;

        public static ClubAuthorityDto.Response of(Long clubId,ClubAuthority clubAuthority) {
            return Response.builder()
                    .id(clubAuthority.getId())
                    .clubId(clubId)
                    .name(clubAuthority.getName())
                    .description(clubAuthority.getDescription())
                    .authorities(clubAuthority.clubAuthorityTypeToString())
                    .build();
        }

        public static List<Response> ofList(Long clubId, List<ClubAuthority> clubAuthorities) {
            return clubAuthorities.stream().map(clubAuthority -> Response.of(clubId, clubAuthority)).collect(Collectors.toList());
        }
    }
}
