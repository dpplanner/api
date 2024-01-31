package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class AuthenticationException extends BaseException {
    private final ErrorResult errorResult;

    public AuthenticationException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
