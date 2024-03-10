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
    RESERVATION_UNAVAILABLE(HttpStatus.BAD_REQUEST,"예약할 수 없는 시간입니다."),
    CLUBMEMBER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "clubMember is already exists, request is invalid"),
    REQUEST_IS_INVALID(HttpStatus.BAD_REQUEST, "request is invalid"),



    /**
     * 401 - 인증되지 않음
     */
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "token is invalid."),

    /**
     * 403 - 권한 없음 -> 404 (to hide resource existence, change 403 -> 404 )
     */
    AUTHORIZATION_DENIED(HttpStatus.NOT_FOUND,"authorization is denied, request is invalid" ),
    READ_AUTHORIZATION_DENIED(HttpStatus.NOT_FOUND,"read authorization is denied, request is invalid" ),
    UPDATE_AUTHORIZATION_DENIED(HttpStatus.NOT_FOUND,"update authorization is denied, request is invalid" ),
    DELETE_AUTHORIZATION_DENIED(HttpStatus.NOT_FOUND, "delete authorization is denied, request is invalid"),
    DIFFERENT_CLUB_EXCEPTION(HttpStatus.NOT_FOUND,"clubmember is different with request resource, request is invalid"),
    CLUBMEMBER_NOT_CONFIRMED(HttpStatus.NOT_FOUND, "clubmember is not confirmed, request is invalid"),


    /**
     * 404 - 데이터 없음
     */
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "member is not found, request is invalid"),
    CLUBMEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "clubMember is not found, request is invalid"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "post is not found , request is invalid"),
    CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "club is not found, request is invalid"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "comment is not found, request is invalid"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,"resource is not found, request is invalid" ),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND,"reservation is not found, request is invalid" ),
    LOCK_NOT_FOUND(HttpStatus.NOT_FOUND,"lock is not found, request is invalid" ),
    CLUB_AUTHORITY_NOT_FOUND(HttpStatus.NOT_FOUND,"club authority is not found, request is invalid" ),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "message is not found, request is invalid"),


    /**
     * 500 - 서버 에러
     */
    FILE_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "file upload fail"),
    ;

    private final HttpStatus httpStatus;
    private final String message;


}
