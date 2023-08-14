package com.dp.dplanner.exception;

public abstract class BaseException extends RuntimeException {
    public abstract ErrorResult getErrorResult();
}
