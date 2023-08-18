package com.dp.dplanner.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorResult {

    /**
     * 400 - 요청이 잘못됨
     */
    CREATE_COMMENT_DENIED(HttpStatus.BAD_REQUEST, "comment creation is denied, request is invalid"),
    PERIOD_OVERLAPPED_EXCEPTION(HttpStatus.BAD_REQUEST,"요청하신 날짜가 겹칩니다."),
    WRONG_INVITE_CODE(HttpStatus.BAD_REQUEST, "초대코드가 일치하지 않습니다."),
    CLUBMEMBER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "clubMember is already exists, request is invalid"),

    /**
     * 401 - 인증되지 않음
     */
    CLUBMEMBER_NOT_CONFIRMED(HttpStatus.UNAUTHORIZED, "클럽 멤버가 아직 승인되지 않았습니다."),

    /**
     * 403 - 권한 없음
     */
    READ_AUTHORIZATION_DENIED(HttpStatus.FORBIDDEN,"read authorization is denied, request is invalid" ),
    UPDATE_AUTHORIZATION_DENIED(HttpStatus.FORBIDDEN,"update authorization is denied, request is invalid" ),
    DELETE_AUTHORIZATION_DENIED(HttpStatus.FORBIDDEN, "delete authorization is denied, request is invalid"),
    DIFFERENT_CLUB_EXCEPTION(HttpStatus.FORBIDDEN,"클럽 맴버와 요청한 리소스가 서로 다른 클럽입니다."),

    /**
     * 404 - 데이터 없음
     */
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "member is not found, request is invalid"),
    CLUBMEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "clubMember is not found, request is invalid"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "post is not found , request is invalid"),
    CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "club is not found, request is invalid"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "comment is not found, request is invalid"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,"resource is not found, request is invalid" ),
    LOCK_NOT_FOUND(HttpStatus.NOT_FOUND,"lock is not found, request is invalid" ),

    /**
     * 500 - 서버 에러
     */
    FILE_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 실패"),
    INVITE_CODE_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "초대코드 생성 실패"),
    ;

    private final HttpStatus httpStatus;
    private final String message;


}
