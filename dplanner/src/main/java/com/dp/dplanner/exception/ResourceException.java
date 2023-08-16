package com.dp.dplanner.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ResourceException extends BaseException {
    private final ErrorResult errorResult;
}
