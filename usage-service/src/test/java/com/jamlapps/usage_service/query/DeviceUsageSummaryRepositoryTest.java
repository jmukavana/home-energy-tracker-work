package com.jamlapps.usage_service.query;

import com.jamlapps.usage_service.query.entity.DeviceUsageSummary;
import com.jamlapps.usage_service.query.repository.DeviceUsageSummaryRepository;
import com.jamlapps.usage_service.query.repository.ProcessedUsageEventRepository;
import com.jamlapps.usage_service.testsupport.MySqlTestcontainersBase;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional
class DeviceUsageSummaryRepositoryTest extends MySqlTestcontainersBase {

    @Autowired
    private DeviceUsageSummaryRepository repository;

    @Autowired
    private ProcessedUsageEventRepository processedUsageEventRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    // Reuses this class's existing Spring context/Testcontainers-MySQL instance
    // rather than a second @SpringBootTest class, since a second full context
    // in the same service caused real teardown/startup races between the two
    // Testcontainers instances (Kafka listener from the first context throwing
    // MySQL "communications link failure" while the second context's container
    // was starting).
    @Test
    void resilience4jConfig_bindsNamedInstancesWithConfiguredThresholds() {
        var deviceCb = circuitBreakerRegistry.circuitBreaker("deviceClientCircuitBreaker");
        assertThat(deviceCb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(8);
        assertThat(deviceCb.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(20f);

        var userCb = circuitBreakerRegistry.circuitBreaker("userClientCircuitBreaker");
        assertThat(userCb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(8);

        var deviceRetry = retryRegistry.retry("deviceClientRetry");
        assertThat(deviceRetry.getRetryConfig().getMaxAttempts()).isEqualTo(3);

        var deviceBulkhead = bulkheadRegistry.bulkhead("deviceClientBulkhead");
        assertThat(deviceBulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(10);

        var deviceRateLimiter = rateLimiterRegistry.rateLimiter("deviceClientRateLimiter");
        assertThat(deviceRateLimiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(20);
    }

    @Test
    void upsertIncrement_sameDeviceAndDate_accumulates() {
        Long deviceId = 1001L;
        LocalDate usageDate = LocalDate.of(2026, 1, 1);

        repository.upsertIncrement(deviceId, 1L, usageDate, 5.0);
        repository.upsertIncrement(deviceId, 1L, usageDate, 3.5);

        List<DeviceUsageSummary> rows =
                repository.findByDeviceIdInAndUsageDateBetween(List.of(deviceId), usageDate, usageDate);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEnergyConsumed()).isEqualTo(8.5);
    }

    @Test
    void upsertIncrement_sameDeviceDifferentDates_createsSeparateRows() {
        Long deviceId = 1002L;
        LocalDate day1 = LocalDate.of(2026, 1, 1);
        LocalDate day2 = LocalDate.of(2026, 1, 2);

        repository.upsertIncrement(deviceId, 1L, day1, 4.0);
        repository.upsertIncrement(deviceId, 1L, day2, 6.0);

        List<DeviceUsageSummary> rows =
                repository.findByDeviceIdInAndUsageDateBetween(List.of(deviceId), day1, day2);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(DeviceUsageSummary::getEnergyConsumed)
                .containsExactlyInAnyOrder(4.0, 6.0);
    }

    @Test
    void findByDeviceIdInAndUsageDateBetween_filtersOutOfRangeAndOtherDevices() {
        Long deviceId = 1003L;
        Long otherDeviceId = 1004L;
        LocalDate inRange = LocalDate.of(2026, 2, 10);
        LocalDate outOfRange = LocalDate.of(2026, 1, 1);

        repository.upsertIncrement(deviceId, 1L, inRange, 10.0);
        repository.upsertIncrement(deviceId, 1L, outOfRange, 99.0);
        repository.upsertIncrement(otherDeviceId, 2L, inRange, 42.0);

        List<DeviceUsageSummary> rows = repository.findByDeviceIdInAndUsageDateBetween(
                List.of(deviceId), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getDeviceId()).isEqualTo(deviceId);
        assertThat(rows.get(0).getEnergyConsumed()).isEqualTo(10.0);
    }

    @Test
    void markProcessed_newEventId_insertsAndReturnsOne() {
        String eventId = java.util.UUID.randomUUID().toString();

        int result = processedUsageEventRepository.markProcessed(eventId);

        assertThat(result).isEqualTo(1);
        assertThat(processedUsageEventRepository.findById(eventId)).isPresent();
    }

    @Test
    void markProcessed_duplicateEventId_returnsZeroAndDoesNotDuplicateInsert() {
        String eventId = java.util.UUID.randomUUID().toString();

        int first = processedUsageEventRepository.markProcessed(eventId);
        int second = processedUsageEventRepository.markProcessed(eventId);

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        assertThat(processedUsageEventRepository.count()).isEqualTo(1);
    }
}
