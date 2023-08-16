package com.dp.dplanner.exception;

import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.ErrorResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ClubMemberException extends BaseException {
    private final ErrorResult errorResult;
}
