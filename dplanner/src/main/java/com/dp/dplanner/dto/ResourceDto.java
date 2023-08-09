package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

public class ResourceDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Create {
        private String name;
        private String info;
        private Long clubId;

        public Resource toEntity(Club club) {
            return Resource.builder()
                    .name(name)
                    .info(info)
                    .club(club)
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
        private Long clubId;

    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response{

        private Long id;
        private String name;
        private String info;
        private Long clubId;

        public static Response of(Resource resource) {
            return Response.builder()
                    .id(resource.getId())
                    .name(resource.getName())
                    .info(resource.getInfo())
                    .clubId(resource.getClub().getId())
                    .build();
        }

        public static List<Response> ofList(List<Resource> resourceList) {

            return resourceList.stream().map(Response::of).collect(Collectors.toList());
        }

    }
}
