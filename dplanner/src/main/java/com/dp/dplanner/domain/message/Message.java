package com.dp.dplanner.domain.message;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
@Setter
public class Message {

    private String title;
    private String content;
    private String redirectUrl;

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
    public static final String POST_REPORTED = "신고된 게시글을 검토해주세요!";
    public static final String POST_DELETED = "게시글이 관리자에 의해 삭제되었습니다!";
    public static final String  NOTICE_REGISTERED = "공지가 등록되었습니다!";
    public static final String COMMENT_ALERT = "내 게시글에 댓글이 등록되었습니다!";
    public static final String CLUB_JOIN = "클럽 가입 요청이 있습니다!";



    @Builder
    public Message(String title, String content, String redirectUrl) {
        this.title = title;
        this.content = content;
        this.redirectUrl = redirectUrl;
    }

    public static Message clubJoinMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(CLUB_JOIN)
                .content(String.format("%s님이 %s클럽에 가입을 요청했습니다.",
                        contentDto.getClubMemberName(), contentDto.getClubName()))
                .redirectUrl("/club_member_list")
                .build();
    }

    public static Message noticeRegisterdMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(NOTICE_REGISTERED)
                .content(String.format("%s클럽에 새로운 공지글이 등록되었습니다.", contentDto.getClubName()))
                .redirectUrl("/tab2")
                .build();
    }

    public static Message commentMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(COMMENT_ALERT)
                .content(String.format("%s님이 %s게시글에 댓글을 남겼습니다.",
                        contentDto.getClubMemberName(), contentDto.getPostTitle()))
                .redirectUrl("/tab2")
                .build();
    }

    public static Message postDeletedMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(POST_DELETED)
                .content(String.format("%s게시글이 관리자에 의해 삭제되었습니다.", contentDto.getPostTitle()))
                .redirectUrl("/tab2")
                .build();
    }

    public static Message postReportedMessage(MessageContentBuildDto contentDto) {
        return Message.builder()
                .title(POST_REPORTED)
                .content(String.format("%s게시글에 대한 신고가 접수되었습니다.", contentDto.getPostTitle()))
                .redirectUrl("/tab2")
                .build();
    }

    public static Message requestMessage(MessageContentBuildDto contentDto) {
        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_REQUEST)
                .content(String.format("%s님이 %s일 %s ~ %s %s 예약을 요청했습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/reservation_list")
                .build();
    }

    public static Message confirmMessage(MessageContentBuildDto contentDto) {
        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_REQUEST_APPROVED)
                .content(String.format("%s님의 %s일 %s ~ %s %s 예약이 승인되었습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message rejectMessage(MessageContentBuildDto contentDto) {
        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_REJECTED)
                .content(String.format("%s님의 %s일 %s ~ %s %s 예약이 거절되었습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation'")
                .build();
    }

    public static Message invitedMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_INVITED)
                .content(String.format("%s님의 %s일 %s ~ %s %s 예약에 초대되었습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message cancelMessage(MessageContentBuildDto contentDto) {
        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_CANCELED)
                .content(String.format("%s님의 %s일 %s ~ %s %s 예약이 관리자에 의해 취소되었습니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation'")
                .build();
    }


    public static Message aboutToStartMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_ABOUT_TO_START)
                .content(String.format("%s님의 %s일 %s ~ %s %s 예약이 10분 후에 시작됩니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message aboutToFinishMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(RESERVATION_ABOUT_TO_FINISH)
                .content(String.format("%s님의 %s일 %s ~ %s %s 예약이 10분 후에 종료됩니다.",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message requestReturnMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(REQUEST_TO_SEND_RETURN_MESSAGE)
                .content(String.format("%s님의 %s일 %s ~ %s %s 예약에 대한 반납 메시지를 보내주세요!",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message checkReturnMessage(MessageContentBuildDto contentDto) {

        String date = contentDto.getStart().format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN));
        String startTime = contentDto.getStart().format(DateTimeFormatter.ofPattern("H시 m분"));
        String endTime = contentDto.getEnd().format(DateTimeFormatter.ofPattern("H시 m분"));

        return Message.builder()
                .title(REQUEST_TO_CHECK_RETURN_MESSAGE)
                .content(String.format("%s님의 %s일 %s ~ %s %s 예약에 대한 반납 메시지가 도착했습니다.!",
                        contentDto.getClubMemberName(), date, startTime, endTime, contentDto.getResourceName()))
                .redirectUrl("/reservation_list")
                .build();
    }

    @Getter
    @Setter
    @Builder
    public static class MessageContentBuildDto{
        String clubName;
        String clubMemberName;
        String postTitle;
        String resourceName;

        LocalDateTime start;
        LocalDateTime end;

    }
}
