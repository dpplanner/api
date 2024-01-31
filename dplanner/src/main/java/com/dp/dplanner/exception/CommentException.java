package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class CommentException extends BaseException {
    private final ErrorResult errorResult;

    public CommentException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
