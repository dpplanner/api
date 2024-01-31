package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class MemberException extends BaseException {
    private final ErrorResult errorResult;
    public MemberException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
