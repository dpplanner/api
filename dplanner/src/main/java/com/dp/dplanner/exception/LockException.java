package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class LockException extends BaseException {
    private final ErrorResult errorResult;
    public LockException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
