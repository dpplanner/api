package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class ApiException extends BaseException{
    private final ErrorResult errorResult;

    public ApiException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
