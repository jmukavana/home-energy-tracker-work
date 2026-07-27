package com.jamlapps.insight_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InsightRestExceptionHandler {

    @ExceptionHandler(UsageServiceUnavailableException.class)
    public ProblemDetail handleUsageServiceUnavailable(UsageServiceUnavailableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problemDetail.setTitle("Usage service unavailable");
        return problemDetail;
    }
}
