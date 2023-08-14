package com.dp.dplanner.exception.post;

import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ErrorResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PostException extends BaseException {
    private final ErrorResult errorResult;
}
