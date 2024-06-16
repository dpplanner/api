package com.dp.dplanner.domain.message;

import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.dp.dplanner.domain.message.InfoType.*;

@Data
@AllArgsConstructor
@Builder
public class Message {

    private String title;
    private String content;
    private String redirectUrl;
    private String info;
    private InfoType infoType;
    private MessageType type;

    public static final String RESERVATION_REQUEST_APPROVED = "예약이 승인되었습니다!";
    public static final String RESERVATION_UPDATE_APPROVED = "예약 수정 요청이 승인되었습니다!";
    public static final String RESERVATION_REJECTED = "예약이 거절되었습니다!";
    public static final String RESERVATION_CANCELED = "예약이 관리자에 의해 취소되었습니다!";
    public static final String RESERVATION_INVITED = "예약에 초대되었습니다!";
    public static final String RESERVATION_REQUEST = "새로운 예약 요청이 있습니다!";
    public static final String RESERVATION_ALERT = "오늘 신청한 예약이 있어요!";
    public static final String RESERVATION_ABOUT_TO_START = "예약이 곧 시작됩니다!";
    public static final String RESERVATION_ABOUT_TO_FINISH = "예약이 곧 종료됩니다!";
    public static final String REQUEST_TO_SEND_RETURN_MESSAGE = "아직 반납 메시지를 보내지 않았어요!";
    public static final String REQUEST_TO_CHECK_RETURN_MESSAGE = "반납 메시지가 도착했습니다!";
    public static final String POST_REPORTED_ADMIN = "신고된 게시글을 검토해주세요!";
    public static final String POST_REPORTED_USER = "게시글이 신고되었어요!";
    public static final String COMMENT_REPORTED_ADMIN = "신고된 댓글을 검토해주세요!";
    public static final String COMMENT_REPORTED_USER = "댓글이 신고되었어요!";
    public static final String POST_DELETED = "게시글이 관리자에 의해 삭제되었습니다!";
    public static final String  NOTICE_REGISTERED = "공지가 등록되었습니다!";
    public static final String COMMENT_ALERT = "내 게시글에 댓글이 등록되었습니다!";
    public static final String CLUB_JOIN = "클럽 가입 요청이 있습니다!";
    public static final String CLUB_JOIN_CONFIRM = "클럽 가입이 승인되었습니다!";
    public static final String CLUB_JOIN_REJECT = "클럽 가입이 거절되었습니다!";


    public static Message clubJoinMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(CLUB_JOIN)
                .content(String.format("%s님이 %s클럽에 가입을 요청했습니다.",
                        contentDto.getClubMemberName(), contentDto.getClubName()))
                .redirectUrl("/club_member_list")
                .infoType(MEMBER)
                .type(MessageType.REQUEST)
                .info(contentDto.info)
                .build();
    }

    public static Message clubJoinConfirmMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(CLUB_JOIN_CONFIRM)
                .content(String.format("%s클럽에 가입이 승인되었습니다.",contentDto.getClubName()))
                .redirectUrl("/club_member_list")
                .infoType(MEMBER)
                .type(MessageType.ACCEPT)
                .info(contentDto.info)
                .build();
    }

    public static Message clubJoinRejectMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(CLUB_JOIN_REJECT)
                .content(String.format("%s클럽에 가입이 거절되었습니다.",contentDto.getClubName()))
                .redirectUrl("/club_member_list")
                .infoType(MEMBER)
                .type(MessageType.REJECT)
                .info(contentDto.info)
                .build();
    }

    public static Message noticeRegisterdMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(NOTICE_REGISTERED)
                .content(String.format("%s클럽에 새로운 공지글이 등록되었습니다.", contentDto.getClubName()))
                .redirectUrl("/tab2")
                .infoType(POST)
                .type(MessageType.NOTICE)
                .info(contentDto.info)
                .build();
    }

    public static Message commentMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(COMMENT_ALERT)
                .content(String.format("%s님이 %s게시글에 댓글을 남겼습니다.",
                        contentDto.getClubMemberName(), contentDto.getPostTitle()))
                .redirectUrl("/tab2")
                .infoType(POST)
                .type(MessageType.INFO)
                .info(contentDto.info)
                .build();
    }

    // info null
    public static Message postDeletedMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(POST_DELETED)
                .content(String.format("%s게시글이 관리자에 의해 삭제되었습니다.", contentDto.getPostTitle()))
                .redirectUrl("/tab2")
                .infoType(POST)
                .type(MessageType.INFO)
                .build();
    }

    public static Message postReportedMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(POST_REPORTED_USER)
                .content(String.format("%s게시글에 대한 신고가 접수되었습니다.", contentDto.getPostTitle()))
                .redirectUrl("/tab2")
                .infoType(POST)
                .type(MessageType.REPORT)
                .info(contentDto.info)
                .build();
    }

    public static Message postReportedADMINMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(POST_REPORTED_ADMIN)
                .content(String.format("%s게시글에 대한 신고가 접수되었습니다.", contentDto.getPostTitle()))
                .redirectUrl("/tab2")
                .infoType(POST)
                .type(MessageType.REPORT)
                .info(contentDto.info)
                .build();
    }

    public static Message commentReportedMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(COMMENT_REPORTED_USER)
                .content("댓글에 대한 신고가 접수되었습니다.")
                .redirectUrl("/tab2")
                .infoType(POST)
                .type(MessageType.REPORT)
                .info(contentDto.info)
                .build();
    }

    public static Message commentReportedADMINMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(COMMENT_REPORTED_ADMIN)
                .content("댓글에 대한 신고가 접수되었습니다.")
                .redirectUrl("/tab2")
                .infoType(POST)
                .type(MessageType.REPORT)
                .info(contentDto.info)
                .build();
    }


    public static Message requestMessage(MessageContentBuildDto contentDto) {
        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_REQUEST)
                .content(String.format("%s님이 %s %s ~ %s %s 예약을 요청했습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/reservation_list")
                .infoType(RESERVATION)
                .type(MessageType.REQUEST)
                .info(contentDto.info)
                .build();
    }

    public static Message confirmMessage(MessageContentBuildDto contentDto) {
        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_REQUEST_APPROVED)
                .content(String.format("%s님의 %s %s ~ %s %s 예약이 승인되었습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .infoType(RESERVATION)
                .type(MessageType.ACCEPT)
                .info(contentDto.info)
                .build();
    }

    public static Message rejectMessage(MessageContentBuildDto contentDto) {
        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_REJECTED)
                .content(String.format("%s님의 %s %s ~ %s %s 예약이 거절되었습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation'")
                .infoType(RESERVATION)
                .type(MessageType.REJECT)
                .info(contentDto.info)
                .build();
    }

    public static Message invitedMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_INVITED)
                .content(String.format("%s님의 %s %s ~ %s %s 예약에 초대되었습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .infoType(RESERVATION)
                .type(MessageType.INFO)
                .info(contentDto.info)
                .build();
    }

    //info null
    public static Message cancelMessage(MessageContentBuildDto contentDto) {
        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_CANCELED)
                .content(String.format("%s님의 %s %s ~ %s %s 예약이 관리자에 의해 취소되었습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation'")
                .infoType(RESERVATION)
                .type(MessageType.REJECT)
                .build();
    }

    public static Message todayReservationMessage(MessageContentBuildDto contentDto) {

        return Message.builder()
                .title(RESERVATION_ALERT)
                .content(String.format("오늘 신청한 예약이 있어요. 확인해 보세요!"))
                .redirectUrl("/my_reservation")
                .infoType(RESERVATION)
                .type(MessageType.INFO)
                .info(contentDto.info)
                .build();
    }

    public static Message aboutToStartMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_ABOUT_TO_START)
                .content(String.format("%s님의 %s %s ~ %s %s 예약이 10분 후에 시작됩니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .infoType(RESERVATION)
                .type(MessageType.INFO)
                .info(contentDto.info)
                .build();
    }

    public static Message aboutToFinishMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_ABOUT_TO_FINISH)
                .content(String.format("%s님의 %s %s ~ %s %s 예약이 10분 후에 종료됩니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .infoType(RESERVATION)
                .type(MessageType.INFO)
                .info(contentDto.info)
                .build();
    }

    public static Message requestReturnMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(REQUEST_TO_SEND_RETURN_MESSAGE)
                .content(String.format("%s님의 %s %s ~ %s %s 예약에 대한 반납 메시지를 보내주세요!",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .infoType(RESERVATION)
                .type(MessageType.REQUEST)
                .info(contentDto.info)
                .build();
    }

    public static Message checkReturnMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(REQUEST_TO_CHECK_RETURN_MESSAGE)
                .content(String.format("%s님의 %s %s ~ %s %s 예약에 대한 반납 메시지가 도착했습니다.!",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/reservation_list")
                .infoType(RESERVATION)
                .type(MessageType.INFO)
                .info(contentDto.info)
                .build();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
//    @AllArgsConstructor
    public static class MessageContentBuildDto {
        String clubName;
        String clubMemberName;
        String postTitle;
        String resourceName;

        LocalDateTime start;
        LocalDateTime end;
        String info;

        @Builder
        public MessageContentBuildDto(String clubName, String clubMemberName, String postTitle, String resourceName, LocalDateTime start, LocalDateTime end, String info) {
            this.clubName = truncateString(clubName, 10);
            this.clubMemberName = truncateString(clubMemberName, 10);
            this.postTitle = truncateString(postTitle, 10);
            this.resourceName = truncateString(resourceName, 10);
            this.start = start;
            this.end = end;
            this.info = info;
        }

        private String truncateString(String value, int maxLength) {
            if (value == null) {
                return "";
            }
            return value.length() < maxLength ? value : value.substring(0, maxLength) + "...";
        }
    }
}
