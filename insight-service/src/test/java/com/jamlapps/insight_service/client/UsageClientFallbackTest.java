package com.jamlapps.insight_service.client;

import com.jamlapps.insight_service.exception.UsageServiceUnavailableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsageClientFallbackTest {

    private final UsageClient usageClient = new UsageClient("http://localhost:8083/api/v1/usage");

    @Test
    void getXDaysUsageForUserFallback_throwsUsageServiceUnavailableException() {
        Long userId = 7L;

        assertThatThrownBy(() -> usageClient.getXDaysUsageForUserFallback(userId, 3, new RuntimeException("boom")))
                .isInstanceOf(UsageServiceUnavailableException.class)
                .hasMessageContaining("7");
    }
}
