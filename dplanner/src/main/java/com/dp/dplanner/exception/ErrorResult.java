package com.dp.dplanner.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorResult {

    CLUBMEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "clubMember is not found, request is invalid"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "post is not found , request is invalid"),
    CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "club is not found, request is invalid"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "comment is not found, request is invalid"),


    UPDATE_AUTHORIZATION_DENIED(HttpStatus.FORBIDDEN,"update authorization is denied, request is invalid" ),
    DELETE_AUTHORIZATION_DENIED(HttpStatus.FORBIDDEN, "delete authorization is denied, request is invalid"),

    CREATE_COMMENT_DENIED(HttpStatus.BAD_REQUEST, "comment creation is denied, request is invalid"),


    ;

    private final HttpStatus httpStatus;
    private final String message;


}
