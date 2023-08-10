package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LockDto{

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Create{

        Long resourceId;
        LocalDateTime startDateTime;
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
        LocalDateTime startDateTime;
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
    public static class Update{
        Long id;
        Long resourceId;
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
    }
}
