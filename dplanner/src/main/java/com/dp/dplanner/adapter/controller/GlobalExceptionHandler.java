package com.dp.dplanner.adapter.controller;

import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.exception.BaseException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errorList = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();
        log.warn("Invalid DTO Parameter errors : {}", errorList);
        CommonResponse commonResponse = this.makeErrorResponseEntity(errorList.toString());
        return handleExceptionInternal(ex, commonResponse, headers, status, request);
    }

    @ExceptionHandler(Throwable.class)
    public CommonResponse handleException(Throwable throwable, HttpServletResponse response) {
        log.error("throwable {}", throwable);
        if (throwable instanceof BaseException) {
            var e = (BaseException) throwable;
            response.setStatus(e.getErrorCode());
            return this.makeErrorResponseEntity(e.getMessage());
        }else{
            var e = throwable;
            response.setStatus(500);
            return this.makeErrorResponseEntity(e.getMessage());
        }
    }

    private CommonResponse makeErrorResponseEntity(String errorDescription) {
        return CommonResponse.createFail(errorDescription);
    }

}
