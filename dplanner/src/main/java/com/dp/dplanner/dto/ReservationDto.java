package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.ClubMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

public class ReservationDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Create {
        private Long resourceId;
        private String title;
        private String usage;
        private boolean sharing;
        private LocalDateTime startDateTime;
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
    public static class Update {
        private Long reservationId;
        private Long resourceId;
        private String title;
        private String usage;
        private boolean sharing;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;

    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Delete {
        private Long reservationId;

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
        private LocalDateTime startDateTime;
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

    }

}
