package com.dp.dplanner.domain.message;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Message {

    private String title;
    private String content;
    private String redirectUrl;

    public static final String RESERVATION_REQUEST_APPROVED = "예약 요청이 승인되었습니다!";
    public static final String RESERVATION_UPDATE_APPROVED = "예약 수정 요청이 승인되었습니다!";
    public static final String RESERVATION_DISCARD = "예약 요청이 취소되었습니다.";
    public static final String RESERVATION_REMOVED = "예약이 삭제되었습니다."; // 삭제랑 취소? 차이가 불분명
    public static final String RESERVATION_INVITED = "예약에 초대되었습니다!";
    public static final String RESERVATION_REQUEST = "예약 요청이 있습니다!";
    public static final String RESERVATION_ALERT = "오늘 신청한 예약이 있어요!";
    public static final String RESERVATION_ABOUT_TO_START = "예약이 곧 시작됩니다.";
    public static final String RESERVATION_ABOUT_TO_FINISH = "예약이 곧 종료됩니다. 예약이 종료되면 반납 메시지를 보내주세요.";
    public static final String REQUEST_TO_SEND_RETURN_MESSAGE = "반납 메시지를 보내주세요.";
    public static final String REQUEST_TO_CHECK_RETURN_MESSAGE = "반납 메시지를 확인해주세요.";
    public static final String POST_REPORTED = "게시글이 신고가 접수되었습니다.";
    public static final String POST_DELETED = "게시글이 관리자에 의해 삭제되었습니다.";
    public static final String  NOTICE_REGISTERED = "공지사항이 등록되었습니다!";
    public static final String COMMENT_ALERT = "댓글이 달렸어요.";
    public static final String CLUB_JOIN = "클럽 가입 요청이 있어요. 확인해주세요";





    @Builder
    public Message(String title, String content, String redirectUrl) {
        this.title = title;
        this.content = content;
        this.redirectUrl = redirectUrl;
    }

    public static Message discardMessage() {
        return Message.builder()
                .title(RESERVATION_DISCARD)
                .content(RESERVATION_DISCARD)
                .redirectUrl("/reservation_list'")
                .build();
    }

    public static Message confirmMessage() {
        return Message.builder()
                .title(RESERVATION_REQUEST_APPROVED)
                .content(RESERVATION_REQUEST_APPROVED)
                .redirectUrl("/reservation_list")
                .build();
    }

    public static Message requestMessage() {
        return Message.builder()
                .title(RESERVATION_REQUEST)
                .content(RESERVATION_REQUEST)
                .redirectUrl("redirectUrl")
                .build();
    }

    public static Message invitedMessage() {
        return Message.builder()
                .title(RESERVATION_INVITED)
                .content(RESERVATION_INVITED)
                .redirectUrl("/reservation_list")
                .build();
    }

    public static Message aboutToStartMessage() {
        return Message.builder()
                .title(RESERVATION_ABOUT_TO_START)
                .content(RESERVATION_ABOUT_TO_START)
                .redirectUrl("/reservation_list")
                .build();
    }

    public static Message aboutToFinishMessage() {
        return Message.builder()
                .title(RESERVATION_ABOUT_TO_FINISH)
                .content(RESERVATION_ABOUT_TO_FINISH)
                .redirectUrl("/reservation_list")
                .build();
    }

    public static Message checkReturnMessage() {
        return Message.builder()
                .title(REQUEST_TO_CHECK_RETURN_MESSAGE)
                .content(REQUEST_TO_CHECK_RETURN_MESSAGE)
                .redirectUrl("redirectUrl")
                .build();
    }

    public static Message requestReturnMessage() {
        return Message.builder()
                .title(REQUEST_TO_SEND_RETURN_MESSAGE)
                .content(REQUEST_TO_SEND_RETURN_MESSAGE)
                .redirectUrl("/reservation_list")
                .build();
    }

    public static Message clubJoinMessage() {
        return Message.builder()
                .title(CLUB_JOIN)
                .content(CLUB_JOIN)
                .redirectUrl("redirectUrl")
                .build();
    }

    public static Message commentMessage() {
        return Message.builder()
                .title(COMMENT_ALERT)
                .content(COMMENT_ALERT)
                .redirectUrl("redirectUrl")
                .build();
    }
    public static Message postDeletedMessage() {
        return Message.builder()
                .title(POST_REPORTED)
                .content(POST_REPORTED)
                .redirectUrl("redirectUrl")
                .build();
    }
    public static Message postReportedMessage() {
        return Message.builder()
                .title(POST_REPORTED)
                .content(POST_REPORTED)
                .redirectUrl("redirectUrl")
                .build();
    }
}
