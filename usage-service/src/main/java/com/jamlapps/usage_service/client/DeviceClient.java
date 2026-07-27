package com.jamlapps.usage_service.client;

import com.jamlapps.usage_service.dto.DeviceDto;
import com.jamlapps.usage_service.exception.DeviceServiceUnavailableException;
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

import java.util.List;

@Component
@Slf4j
public class DeviceClient {

    private final RestTemplate restTemplate;

    private final String baseUrl;

    public DeviceClient(@Value("${device.service.url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    @CircuitBreaker(name = "deviceClientCircuitBreaker")
    @RateLimiter(name = "deviceClientRateLimiter")
    @Bulkhead(name = "deviceClientBulkhead")
    @Retry(name = "deviceClientRetry", fallbackMethod = "getDeviceByIdFallback")
    public DeviceDto getDeviceById (Long deviceId) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{deviceId}")
                .buildAndExpand(deviceId)
                .toUriString();

        ResponseEntity<DeviceDto> response = restTemplate.getForEntity(url, DeviceDto.class);
        return response.getBody();
    }

    // Called from a background scheduled job (UsageAlertingScheduler) and the
    // read-model projector (UsageProjectionListener), both of which already
    // tolerate a missing device by skipping/logging - preserve that behavior.
    DeviceDto getDeviceByIdFallback(Long deviceId, Exception ex) {
        log.warn("device-service unavailable for deviceId {}: {}", deviceId, ex.getMessage());
        return null;
    }

    @CircuitBreaker(name = "deviceClientCircuitBreaker")
    @RateLimiter(name = "deviceClientRateLimiter")
    @Bulkhead(name = "deviceClientBulkhead")
    @Retry(name = "deviceClientRetry", fallbackMethod = "getAllDevicesForUserFallback")
    public List<DeviceDto> getAllDevicesForUser(Long userId) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/user/{userId}")
                .buildAndExpand(userId)
                .toUriString();

        ResponseEntity<DeviceDto[]> response = restTemplate.getForEntity(url, DeviceDto[].class);
        DeviceDto[] devices = response.getBody();
        return devices == null ? List.of() : List.of(devices);
    }

    // Called from UsageQueryService on a live API request path with no
    // existing try/catch - throw a typed exception rather than silently
    // returning an empty list, so the caller gets an honest 503 instead of
    // a misleading "user has zero devices" response.
    List<DeviceDto> getAllDevicesForUserFallback(Long userId, Exception ex) {
        throw new DeviceServiceUnavailableException(
                "device-service is unavailable while fetching devices for user " + userId, ex);
    }
}
