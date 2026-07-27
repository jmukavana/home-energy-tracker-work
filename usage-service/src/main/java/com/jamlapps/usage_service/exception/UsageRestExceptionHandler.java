package com.jamlapps.usage_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UsageRestExceptionHandler {

    @ExceptionHandler(DeviceServiceUnavailableException.class)
    public ProblemDetail handleDeviceServiceUnavailable(DeviceServiceUnavailableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problemDetail.setTitle("Device service unavailable");
        return problemDetail;
    }
}
