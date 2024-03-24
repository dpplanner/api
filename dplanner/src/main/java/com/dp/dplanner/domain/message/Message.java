package com.dp.dplanner.domain.message;

import lombok.Builder;
import lombok.Getter;

@Getter
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
    public static final String RESERVATION_ABOUT_TO_FINISH = "예약이 곧 종료됩니다! 예약이 종료되면 반납 메시지를 보내주세요.";
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

    public static Message rejectMessage() {
        return Message.builder()
                .title(RESERVATION_REJECTED)
                .content(RESERVATION_REJECTED)
                .redirectUrl("/my_reservation'")
                .build();
    }

    public static Message cancelMessage() {
        return Message.builder()
                .title(RESERVATION_CANCELED)
                .content(RESERVATION_CANCELED)
                .redirectUrl("/my_reservation'")
                .build();
    }

    public static Message confirmMessage() {
        return Message.builder()
                .title(RESERVATION_REQUEST_APPROVED)
                .content(RESERVATION_REQUEST_APPROVED)
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message requestMessage() {
        return Message.builder()
                .title(RESERVATION_REQUEST)
                .content(RESERVATION_REQUEST)
                .redirectUrl("/reservation_list")
                .build();
    }

    public static Message invitedMessage() {
        return Message.builder()
                .title(RESERVATION_INVITED)
                .content(RESERVATION_INVITED)
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message aboutToStartMessage() {
        return Message.builder()
                .title(RESERVATION_ABOUT_TO_START)
                .content(RESERVATION_ABOUT_TO_START)
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message aboutToFinishMessage() {
        return Message.builder()
                .title(RESERVATION_ABOUT_TO_FINISH)
                .content(RESERVATION_ABOUT_TO_FINISH)
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message checkReturnMessage() {
        return Message.builder()
                .title(REQUEST_TO_CHECK_RETURN_MESSAGE)
                .content(REQUEST_TO_CHECK_RETURN_MESSAGE)
                .redirectUrl("/reservation_list")
                .build();
    }

    public static Message requestReturnMessage() {
        return Message.builder()
                .title(REQUEST_TO_SEND_RETURN_MESSAGE)
                .content(REQUEST_TO_SEND_RETURN_MESSAGE)
                .redirectUrl("/my_reservation")
                .build();
    }

    public static Message clubJoinMessage() {
        return Message.builder()
                .title(CLUB_JOIN)
                .content(CLUB_JOIN)
                .redirectUrl("/club_member_list")
                .build();
    }

    public static Message commentMessage() {
        return Message.builder()
                .title(COMMENT_ALERT)
                .content(COMMENT_ALERT)
                .redirectUrl("/tab2")
                .build();
    }
    public static Message postDeletedMessage() {
        return Message.builder()
                .title(POST_REPORTED)
                .content(POST_REPORTED)
                .redirectUrl("/tab2")
                .build();
    }
    public static Message postReportedMessage() {
        return Message.builder()
                .title(POST_REPORTED)
                .content(POST_REPORTED)
                .redirectUrl("/tab2")
                .build();
    }

    public static Message noticeRegisterdMessage() {
        return Message.builder()
                .title(NOTICE_REGISTERED)
                .content(NOTICE_REGISTERED)
                .redirectUrl("/tab2")
                .build();
    }
}
