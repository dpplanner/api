package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class ResourceException extends BaseException {
    private final ErrorResult errorResult;
    public ResourceException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
