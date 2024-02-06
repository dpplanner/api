package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class MessageException extends BaseException {
    private final ErrorResult errorResult;

    public MessageException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
