package com.dp.dplanner.exception;

import com.dp.dplanner.dto.CommonResponse;
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

    @ExceptionHandler({Exception.class})
    public CommonResponse handleException(final Exception exception,HttpServletResponse response) {
        log.warn("Exception occur");
        response.setStatus(500);
        return this.makeErrorResponseEntity(exception.getMessage());
    }

    @ExceptionHandler({PostException.class})
    public CommonResponse handleRestApiException(final PostException exception, HttpServletResponse response) {
        log.warn("PostException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({ClubMemberException.class})
    public CommonResponse handleRestApiException(final ClubMemberException exception, HttpServletResponse response) {
        log.warn("ClubMemberException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({MemberException.class})
    public CommonResponse handleRestApiException(final MemberException exception, HttpServletResponse response) {
        log.warn("MemberException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({ClubException.class})
    public CommonResponse handleRestApiException(final ClubException exception,HttpServletResponse response) {
        log.warn("ClubException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({CommentException.class})
    public CommonResponse handleRestApiException(final CommentException exception,HttpServletResponse response) {
        log.warn("CommentException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({ResourceException.class})
    public CommonResponse handleRestApiException(final ResourceException exception,HttpServletResponse response) {
        log.warn("ResourceException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }
    @ExceptionHandler({LockException.class})
    public CommonResponse handleRestApiException(final LockException exception,HttpServletResponse response) {
        log.warn("LockException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }
    @ExceptionHandler({ReservationException.class})
    public CommonResponse handleRestApiException(final ReservationException exception,HttpServletResponse response) {
        log.warn("ReservationException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({InviteCodeException.class})
    public CommonResponse handleRestApiException(final InviteCodeException exception,HttpServletResponse response) {
        log.warn("InviteCodeException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({AuthenticationException.class})
    public CommonResponse handleRestApiException(final AuthenticationException exception,HttpServletResponse response) {
        log.warn("AuthenticationException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({ClubAuthorityException.class})
    public CommonResponse handleRestApiException(final ClubAuthorityException exception,HttpServletResponse response) {
        log.warn("ClubAuthorityException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({MessageException.class})
    public CommonResponse handleRestApiException(final MessageException exception,HttpServletResponse response) {
        log.warn("MessageException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }


    private CommonResponse makeErrorResponseEntity(BaseException baseException, HttpServletResponse response) {
        response.setStatus(baseException.getErrorResult().getHttpStatus().value());
        return CommonResponse.createFail(baseException.getMessage());
    }

    private CommonResponse makeErrorResponseEntity(String errorDescription) {
        return CommonResponse.createFail(errorDescription);
    }

}
