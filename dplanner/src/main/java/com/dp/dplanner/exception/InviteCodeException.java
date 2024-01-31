package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class InviteCodeException extends BaseException {
    private final ErrorResult errorResult;
    public InviteCodeException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
