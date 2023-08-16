package com.dp.dplanner.exception.resource;

import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ErrorResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ResourceException extends BaseException {
    private final ErrorResult errorResult;
}
