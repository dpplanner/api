package com.dp.dplanner.exception.lock;

import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ErrorResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LockException extends BaseException {
    private final ErrorResult errorResult;
}
