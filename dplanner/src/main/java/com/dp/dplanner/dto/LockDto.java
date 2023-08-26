package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LockDto{

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Create{

        Long resourceId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime startDateTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime endDateTime;


        public Lock toEntity(Resource resource) {
            return Lock.builder()
                    .resource(resource)
                    .period(new Period(startDateTime,endDateTime))
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response{

        Long id;
        Long resourceId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime startDateTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime endDateTime;

        public static Response of(Lock lock) {
            return Response.builder()
                    .id(lock.getId())
                    .startDateTime(lock.getPeriod().getStartDateTime())
                    .endDateTime(lock.getPeriod().getEndDateTime())
                    .resourceId(lock.getResource().getId())
                    .build();
        }

        public static List<Response> ofList(List<Lock> locks) {

            return locks.stream().map(Response::of).collect(Collectors.toList());
        }
    }
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Update{
        Long id;
        Long resourceId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime startDateTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime endDateTime;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request{
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime startDateTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime endDateTime;
    }
}
