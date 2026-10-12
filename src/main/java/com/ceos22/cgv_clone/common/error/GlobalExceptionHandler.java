package com.ceos22.cgv_clone.common.error;

import com.ceos22.cgv_clone.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException e) {
        log.atDebug()
                .addKeyValue("event", "api.request.rejected")
                .addKeyValue("errorCode", e.getErrorCode().getCode())
                .addKeyValue("statusCode", e.getErrorCode().getStatus().value())
                .log();
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        if ((e instanceof ErrorResponse error && error.getStatusCode().is4xxClientError())
                || e instanceof TypeMismatchException
                || e instanceof HttpMessageNotReadableException) {
            ErrorCode errorCode = e instanceof TypeMismatchException ? ErrorCode.INVALID_TYPE_VALUE
                    : e instanceof HttpMessageNotReadableException ? ErrorCode.INVALID_INPUT_VALUE
                    : ErrorCode.INTERNAL_SERVER_ERROR;
            log.atDebug()
                    .addKeyValue("event", "api.request.rejected")
                    .addKeyValue("errorCode", errorCode.getCode())
                    .addKeyValue("statusCode", ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
                    .log();
        } else {
            log.atError()
                    .addKeyValue("event", "api.request.errored")
                    .addKeyValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                    .addKeyValue("statusCode", ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
                    .setCause(e)
                    .log();
        }
        return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
