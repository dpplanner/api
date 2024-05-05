package com.dp.dplanner.exception;

public abstract class BaseException extends RuntimeException {
    public abstract ErrorResult getErrorResult();
    public BaseException(ErrorResult errorResult) {
        super(errorResult.getMessage());
    }
    public BaseException(String message) {
        super(message);
    }
}
