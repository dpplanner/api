package com.dp.dplanner.exception;

import lombok.Getter;

@Getter
public class ReservationException extends BaseException {
    private final ErrorResult errorResult;
    public ReservationException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
