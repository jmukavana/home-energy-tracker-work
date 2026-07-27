package com.jamlapps.insight_service.client;

import com.jamlapps.insight_service.dto.UsageDto;
import com.jamlapps.insight_service.exception.UsageServiceUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class UsageClient {

    private final RestTemplate restTemplate;

    private final String baseUrl;

    public UsageClient(@Value("${usage.service.url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    @CircuitBreaker(name = "usageClientCircuitBreaker")
    @RateLimiter(name = "usageClientRateLimiter")
    @Bulkhead(name = "usageClientBulkhead")
    @Retry(name = "usageClientRetry", fallbackMethod = "getXDaysUsageForUserFallback")
    public UsageDto getXDaysUsageForUser (Long userId, int days) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{userId}")
                .queryParam("days", days)
                .buildAndExpand(userId)
                .toUriString();

        ResponseEntity<UsageDto> response = restTemplate.getForEntity(url, UsageDto.class);
        return response.getBody();
    }

    // Both callers (InsightService.getSavingsTips/getOverview) dereference
    // usageData.devices() with no null-check - a null fallback would become
    // an uncaught NPE instead of a clean error, so throw a typed exception.
    UsageDto getXDaysUsageForUserFallback(Long userId, int days, Exception ex) {
        throw new UsageServiceUnavailableException(
                "usage-service is unavailable while fetching usage for user " + userId, ex);
    }
}
