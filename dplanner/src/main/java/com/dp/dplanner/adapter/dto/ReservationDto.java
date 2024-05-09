package com.dp.dplanner.adapter.dto;

import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.ClubMember;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        @Builder.Default
        private List<Long> reservationInvitees = new ArrayList<>();

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
        @Builder.Default
        private List<Long> reservationInvitees = new ArrayList<>();
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
        private Long clubId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime startDateTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime endDateTime;
        private String rejectMessage;

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
        private String clubMemberName;
        private Long resourceId;
        private String resourceName;
        private String title;
        private String usage;
        private boolean sharing;
        private String  status;
        private boolean isReturned;
        private String returnMessage;
        private List<String> attachmentsUrl;
        @Builder.Default
        private List<ReservationInviteeDto> invitees = new ArrayList<>();
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
                    .clubMemberName(reservation.getClubMember().getName())
                    .resourceId(reservation.getResource().getId())
                    .resourceName(reservation.getResource().getName())
                    .title(reservation.getTitle())
                    .usage(reservation.getUsage())
                    .sharing(reservation.isSharing())
                    .status(reservation.getStatus().name())
                    .startDateTime(reservation.getPeriod().getStartDateTime())
                    .endDateTime(reservation.getPeriod().getEndDateTime())
                    .createDate(reservation.getCreatedDate())
                    .lastModifiedDate(reservation.getLastModifiedDate())
                    .isReturned(reservation.isReturned())
                    .returnMessage(reservation.getReturnMessage())
                    .attachmentsUrl(reservation.getAttachments().stream().map(Attachment::getUrl).collect(Collectors.toList()))
                    .invitees(
                            reservation.getReservationInvitees().stream().map(
                                    reservationInvitee ->
                                            ReservationInviteeDto.builder()
                                                    .clubMemberId(reservationInvitee.getClubMember().getId())
                                                    .clubMemberName(reservationInvitee.getClubMember().getName())
                                                    .build()).collect(Collectors.toList())
                    )
                    .build();
        }

        public static List<Response> ofList(List<Reservation> reservations) {
            return reservations.stream().map(Response::of).toList();
        }

    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class SliceResponse {
        private List<Response> content;
        private Pageable pageable;
        private boolean hasNext;
    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Return {
        private Long reservationId;
        private String returnMessage;
        @Builder.Default
        private List<MultipartFile> files = new ArrayList<>();

    }


}

