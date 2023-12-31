package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.ClubMember;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ReservationDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Create {
        private Long resourceId;
        private String title;
        private String usage;
        private boolean sharing;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime startDateTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime endDateTime;

        public Reservation toEntity(ClubMember clubMember, Resource resource) {
            return Reservation.builder()
                    .clubMember(clubMember)
                    .resource(resource)
                    .period(new Period(startDateTime, endDateTime))
                    .title(title)
                    .usage(usage)
                    .sharing(sharing)
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Update {
        private Long reservationId;
        private Long resourceId;
        private String title;
        private String usage;
        private boolean sharing;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime startDateTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime endDateTime;

    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Delete {
        private Long reservationId;

    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        private Long reservationId;
        private Long resourceId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime startDateTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime endDateTime;

        public Request(Long reservationId) {
            this.reservationId = reservationId;
        }

        public Request(Long resourceId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
            this.resourceId = resourceId;
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
        }

        public static List<Request> ofList(List<Long> reservationIds) {
            return reservationIds.stream().map(Request::new).toList();
        }
    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response {

        private Long reservationId;
        private Long clubMemberId;
        private Long resourceId;
        private String title;
        private String usage;
        private boolean sharing;
        private String  status;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime startDateTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime endDateTime;
        private LocalDateTime createDate;
        private LocalDateTime lastModifiedDate;

        public static Response of(Reservation reservation) {
            return Response.builder()
                    .reservationId(reservation.getId())
                    .clubMemberId(reservation.getClubMember().getId())
                    .resourceId(reservation.getResource().getId())
                    .title(reservation.getTitle())
                    .usage(reservation.getUsage())
                    .sharing(reservation.isSharing())
                    .status(reservation.getStatus().name())
                    .startDateTime(reservation.getPeriod().getStartDateTime())
                    .endDateTime(reservation.getPeriod().getEndDateTime())
                    .createDate(reservation.getCreatedDate())
                    .lastModifiedDate(reservation.getLastModifiedDate())
                    .build();
        }

        public static List<Response> ofList(List<Reservation> reservations) {
            return reservations.stream().map(Response::of).toList();
        }

    }
}

