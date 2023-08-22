package com.dp.dplanner.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
        return this.makeErrorResponseEntity(errorList.toString());


    }

    @ExceptionHandler({PostException.class})
    public ResponseEntity<ErrorResponse> handleRestApiException(final PostException exception) {
        log.warn("PostException occurs : ", exception);
        return this.makeErrorResponseEntity(exception);
    }

    @ExceptionHandler({ClubMemberException.class})
    public ResponseEntity<ErrorResponse> handleRestApiException(final ClubMemberException exception) {
        log.warn("ClubMemberException occurs : ", exception);
        return this.makeErrorResponseEntity(exception);
    }

    @ExceptionHandler({MemberException.class})
    public ResponseEntity<ErrorResponse> handleRestApiException(final MemberException exception) {
        log.warn("MemberException occurs : ", exception);
        return this.makeErrorResponseEntity(exception);
    }

    @ExceptionHandler({ClubException.class})
    public ResponseEntity<ErrorResponse> handleRestApiException(final ClubException exception) {
        log.warn("ClubException occurs : ", exception);
        return this.makeErrorResponseEntity(exception);
    }

    private ResponseEntity<ErrorResponse> makeErrorResponseEntity(BaseException baseException) {
        return ResponseEntity.status(baseException.getErrorResult().getHttpStatus())
                .body(new ErrorResponse(baseException.getErrorResult().toString(),
                        baseException.getErrorResult().getMessage()));
    }

    private ResponseEntity<Object> makeErrorResponseEntity(String errorDescription) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.toString(), errorDescription));
    }

}
