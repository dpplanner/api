package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class ClubAuthorityException extends BaseException {
    private final ErrorResult errorResult;
    public ClubAuthorityException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
