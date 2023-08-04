package com.dp.dplanner.dto;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    @AllArgsConstructor
    public static class Update {
        private Long clubId;
        private List<String> authorities;

        public Update(Long clubId, String... authorities) {
            this.clubId = clubId;
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
    public static class Response {
        private Long clubId;
        private List<String> authorities;

        public static ClubAuthorityDto.Response of(Club club) {
            List<String> authorities = club.getManagerAuthorities()
                    .stream()
                    .map(ClubAuthority::getClubAuthorityType)
                    .map(ClubAuthorityType::name)
                    .toList();

            return Response.builder()
                    .clubId(club.getId())
                    .authorities(authorities)
                    .build();
        }
    }
}
