package com.dp.dplanner.adapter.exception;

import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ErrorResult;
import lombok.Getter;

@Getter
public class ApiException extends BaseException {
    private final ErrorResult errorResult;

    public ApiException(ErrorResult errorResult) {
        super(errorResult);
        this.errorResult = errorResult;
    }
}
