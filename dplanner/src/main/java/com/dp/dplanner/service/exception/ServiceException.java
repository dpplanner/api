package com.dp.dplanner.service.exception;

import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ErrorResult;
import lombok.Getter;

@Getter
public class ServiceException  extends BaseException {
    private final String message;
    private final int errorCode;
    private final ErrorResult errorResult;

    public ServiceException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
        this.message = errorResult.getMessage();
        this.errorCode = errorResult.getHttpStatus().value();
    }
    public ServiceException(String message,int errorCode) {
        super(message);
        this.errorResult = null;
        this.message = message;
        this.errorCode = errorCode;
    }
}
