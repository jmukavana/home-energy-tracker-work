package com.jamlapps.usage_service.client;

import com.jamlapps.usage_service.dto.UserDto;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class UserClient {

    private final RestTemplate restTemplate;

    private final String baseUrl;

    public UserClient(@Value("${user.service.url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    @CircuitBreaker(name = "userClientCircuitBreaker")
    @RateLimiter(name = "userClientRateLimiter")
    @Bulkhead(name = "userClientBulkhead")
    @Retry(name = "userClientRetry", fallbackMethod = "getUserByIdFallback")
    public UserDto getUserById(Long userId) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{userId}")
                .buildAndExpand(userId)
                .toUriString();

        ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);
        return response.getBody();
    }

    // Only caller today is UsageAlertingScheduler, which already tolerates a
    // missing user by skipping/logging per-user - preserve that behavior.
    UserDto getUserByIdFallback(Long userId, Exception ex) {
        log.warn("user-service unavailable for userId {}: {}", userId, ex.getMessage());
        return null;
    }
}
