package com.jamlapps.insight_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class InsightRestExceptionHandlerTest {

    private final InsightRestExceptionHandler handler = new InsightRestExceptionHandler();

    @Test
    void handleUsageServiceUnavailable_returns503ProblemDetail() {
        UsageServiceUnavailableException ex = new UsageServiceUnavailableException("usage-service is down");

        ProblemDetail problemDetail = handler.handleUsageServiceUnavailable(ex);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Usage service unavailable");
        assertThat(problemDetail.getDetail()).isEqualTo("usage-service is down");
    }
}
