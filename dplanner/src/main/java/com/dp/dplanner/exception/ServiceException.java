package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class ServiceException  extends BaseException{
    private final ErrorResult errorResult;

    public ServiceException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
