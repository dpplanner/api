package com.dp.dplanner.exception;

import com.dp.dplanner.dto.CommonResponse;
import jakarta.servlet.http.HttpServletResponse;
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
        return this.makeErrorResponseEntity(errorList.toString(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({PostException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final PostException exception, HttpServletResponse response) {
        log.warn("PostException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({ClubMemberException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final ClubMemberException exception, HttpServletResponse response) {
        log.warn("ClubMemberException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({MemberException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final MemberException exception, HttpServletResponse response) {
        log.warn("MemberException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({ClubException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final ClubException exception,HttpServletResponse response) {
        log.warn("ClubException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({CommentException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final CommentException exception,HttpServletResponse response) {
        log.warn("CommentException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({ResourceException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final ResourceException exception,HttpServletResponse response) {
        log.warn("ResourceException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }
    @ExceptionHandler({LockException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final LockException exception,HttpServletResponse response) {
        log.warn("LockException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }
    @ExceptionHandler({ReservationException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final ReservationException exception,HttpServletResponse response) {
        log.warn("ReservationException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({InviteCodeException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final InviteCodeException exception,HttpServletResponse response) {
        log.warn("InviteCodeException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({AuthenticationException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final AuthenticationException exception,HttpServletResponse response) {
        log.warn("AuthenticationException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }

    @ExceptionHandler({ClubAuthorityException.class})
    public CommonResponse<ErrorResponse> handleRestApiException(final ClubAuthorityException exception,HttpServletResponse response) {
        log.warn("ClubAuthorityException occurs : {}", exception.getErrorResult().getMessage());
        return this.makeErrorResponseEntity(exception,response);
    }
    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleException(final Exception exception) {
        log.warn("Exception occur");
        exception.printStackTrace();
        return this.makeErrorResponseEntity(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private CommonResponse<ErrorResponse> makeErrorResponseEntity(BaseException baseException, HttpServletResponse response) {
        response.setStatus(baseException.getErrorResult().getHttpStatus().value());
        return CommonResponse.createFail(baseException.getMessage());
    }

    private ResponseEntity<Object> makeErrorResponseEntity(String errorDescription, HttpStatus httpStatus) {
        return ResponseEntity.status(httpStatus)
                .body(new ErrorResponse(httpStatus.toString(), errorDescription));
    }

}
