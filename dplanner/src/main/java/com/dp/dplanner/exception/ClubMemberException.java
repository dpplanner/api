package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class ClubMemberException extends BaseException {
    private final ErrorResult errorResult;

    public ClubMemberException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
