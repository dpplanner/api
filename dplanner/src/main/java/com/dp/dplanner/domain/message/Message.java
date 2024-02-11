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
    public static final String RESERVATION_INVITED = "예약에 초대되었습니다!";
    public static final String POST_REPORTED = "게시글이 신고가 접수되었습니다.";
    public static final String RESERVATION_REQUEST = "예약 요청이 있습니다!";
    public static final String  NOTICE_REGISTERED = "공지사항이 등록되었습니다!";


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
                .redirectUrl("redirectUrl")
                .build();
    }

    public static Message confirmMessage() {
        return Message.builder()
                .title(RESERVATION_REQUEST_APPROVED)
                .content(RESERVATION_REQUEST_APPROVED)
                .redirectUrl("redirectUrl")
                .build();
    }

    public static Message requestMessage() {
        return Message.builder()
                .title(RESERVATION_REQUEST)
                .content(RESERVATION_REQUEST)
                .redirectUrl("redirectUrl")
                .build();
    }
}
