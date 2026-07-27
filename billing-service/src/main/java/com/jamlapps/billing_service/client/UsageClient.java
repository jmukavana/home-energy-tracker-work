package com.jamlapps.billing_service.client;

import com.jamlapps.billing_service.dto.UsageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Plain RestTemplate call, deliberately without the Resilience4j stack Phase 1a
 * added to usage-service/insight-service's clients: those wrap calls on a
 * background scheduler / hot request path; this one backs a single on-demand
 * admin/demo endpoint (invoice generation), so a clear failure on the caller's
 * request is the right behavior rather than a fallback/retry policy.
 */
@Component
public class UsageClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UsageClient(@Value("${usage.service.url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    public UsageDto getXDaysUsageForUser(Long userId, int days) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{userId}")
                .queryParam("days", days)
                .buildAndExpand(userId)
                .toUriString();

        return restTemplate.getForObject(url, UsageDto.class);
    }
}
