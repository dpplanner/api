package com.dp.dplanner.adapter.dto;

import com.dp.dplanner.domain.club.Club;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ClubDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create{
        @NotNull
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update{
        private Long clubId;
        private String info;

    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request{
        private Long clubId;
    }

    public interface ResponseMapping {
        Long getId();
        String getClubName();
        String getInfo();
        String getUrl();
        Long getMemberCount();
        Boolean getIsConfirmed();
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response{
        private Long id;
        private String clubName;
        private String info;
        private Long memberCount;
        private Boolean isConfirmed;
        private String url;

        public static ClubDto.Response of(ResponseMapping responseMapping) {
            return Response.builder()
                    .id(responseMapping.getId())
                    .clubName(responseMapping.getClubName())
                    .info(responseMapping.getInfo())
                    .memberCount(responseMapping.getMemberCount())
                    .isConfirmed(responseMapping.getIsConfirmed())
                    .url(responseMapping.getUrl())
                    .build();
        }
        public static ClubDto.Response of(Club club) {
            return Response.builder()
                    .id(club.getId())
                    .clubName(club.getClubName())
                    .info(club.getInfo())
                    .url(club.getUrl())
                    .build();
        }
        public static List<ClubDto.Response> ofList(List<Club> clubs) {
            return clubs.stream().map(ClubDto.Response::of).toList();
        }

    }
}
