package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class ClubException extends BaseException {
    private final ErrorResult errorResult;
    public ClubException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
