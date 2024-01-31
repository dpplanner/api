package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class PostException extends BaseException {
    private final ErrorResult errorResult;
    public PostException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
