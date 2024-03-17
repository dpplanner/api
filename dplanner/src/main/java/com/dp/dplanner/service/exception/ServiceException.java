package com.dp.dplanner.service.exception;

import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ErrorResult;
import lombok.Getter;

@Getter
public class ServiceException  extends BaseException {
    private final ErrorResult errorResult;

    public ServiceException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
