package com.dp.dplanner.adapter.exception;

import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ErrorResult;
import lombok.Getter;

@Getter
public class ApiException extends BaseException {
    private final ErrorResult errorResult;
    private final String message;
    private final int errorCode;

    public ApiException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
        this.message = errorResult.getMessage();
        this.errorCode = errorResult.getHttpStatus().value();
    }

    public ApiException(String message,int errorCode) {
        super(message);
        this.errorResult = null;
        this.message = message;
        this.errorCode = errorCode;
    }
}
