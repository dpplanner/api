package com.dp.dplanner.adapter.dto;

import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.ResourceType;
import com.dp.dplanner.domain.club.Club;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

public class ResourceDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Create {
        private String name;
        private String info;
        private Long clubId;
        private String resourceType;
        private String notice;
        private boolean returnMessageRequired;
        private Long bookableSpan;

        public Resource toEntity(Club club) {
            return Resource.builder()
                    .name(name)
                    .info(info)
                    .club(club)
                    .notice(notice)
                    .resourceType(ResourceType.valueOf(resourceType))
                    .returnMessageRequired(returnMessageRequired)
                    .bookableSpan(bookableSpan)
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
        private String name;
        private String info;
        private String resourceType;
        private String notice;
        private boolean returnMessageRequired;
        private Long bookableSpan;

    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response{

        private Long id;
        private String name;
        private String info;
        private boolean returnMessageRequired;
        private String resourceType;
        private String notice;
        private Long clubId;
        private Long bookableSpan;


        public static Response of(Resource resource) {
            return Response.builder()
                    .id(resource.getId())
                    .name(resource.getName())
                    .info(resource.getInfo())
                    .clubId(resource.getClub().getId())
                    .returnMessageRequired(resource.isReturnMessageRequired())
                    .notice(resource.getNotice())
                    .resourceType(resource.getResourceType().toString())
                    .bookableSpan((resource.getBookableSpan()))
                    .build();
        }

        public static List<Response> ofList(List<Resource> resourceList) {

            return resourceList.stream().map(Response::of).collect(Collectors.toList());
        }

    }
}
