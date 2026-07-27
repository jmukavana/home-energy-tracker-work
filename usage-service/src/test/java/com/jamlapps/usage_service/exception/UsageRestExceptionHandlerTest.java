package com.jamlapps.usage_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class UsageRestExceptionHandlerTest {

    private final UsageRestExceptionHandler handler = new UsageRestExceptionHandler();

    @Test
    void handleDeviceServiceUnavailable_returns503ProblemDetail() {
        DeviceServiceUnavailableException ex = new DeviceServiceUnavailableException("device-service is down");

        ProblemDetail problemDetail = handler.handleDeviceServiceUnavailable(ex);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Device service unavailable");
        assertThat(problemDetail.getDetail()).isEqualTo("device-service is down");
    }
}
