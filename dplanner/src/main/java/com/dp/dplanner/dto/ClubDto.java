package com.dp.dplanner.dto;

import com.dp.dplanner.domain.club.Club;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ClubDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Create{
        private String clubName;
        private String info;

        public Club toEntity() {
            return Club.builder()
                    .clubName(this.clubName)
                    .info(this.info)
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Update{
        private Long id;
        private String info;

    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response{
        private Long id;
        private String clubName;
        private String info;

        public static ClubDto.Response of(Club club) {
            return ClubDto.Response.builder()
                    .id(club.getId())
                    .clubName(club.getClubName())
                    .info(club.getInfo())
                    .build();
        }

        public static List<ClubDto.Response> ofList(List<Club> clubs) {
            return clubs.stream().map(ClubDto.Response::of).toList();
        }
    }
}
