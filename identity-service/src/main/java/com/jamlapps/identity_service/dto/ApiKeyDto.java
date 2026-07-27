package com.jamlapps.identity_service.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ApiKeyDto(
        Long id,
        Long userId,
        String label,
        String keyPrefix,
        LocalDateTime createdAt,
        LocalDateTime revokedAt,
        LocalDateTime lastUsedAt
) {
}
