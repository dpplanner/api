package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class FileException extends BaseException {
    private final ErrorResult errorResult;

    public FileException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
