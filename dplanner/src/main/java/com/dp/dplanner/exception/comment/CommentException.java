package com.dp.dplanner.exception.comment;

import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ErrorResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class CommentException extends BaseException {
    private final ErrorResult errorResult;
}
