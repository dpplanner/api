package com.dp.dplanner.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonResponse<T> {

    private static final String SUCCESS_STATUS = "success";
    private static final String FAIL_STATUS = "fail";

    private String status;
    private T data;
    private String message;

    public static <T> CommonResponse<T> createSuccess(T data) {
        return new CommonResponse<>(SUCCESS_STATUS, data, null);
    }

    public static CommonResponse<?> createSuccessWithNoContent() {
        return new CommonResponse<>(SUCCESS_STATUS, null, null);
    }

    public static CommonResponse createFail(String errorMessage) {
        //toDo data에 잘못된 URL, 바디 전달 필요?
        return new CommonResponse<>(FAIL_STATUS, null, errorMessage);
    }

    public CommonResponse(String status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }
}
