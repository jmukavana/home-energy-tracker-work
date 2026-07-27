package com.jamlapps.audit_service.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AuditLogEntryDto(
        Long id,
        String eventId,
        String actorType,
        Long actorId,
        String action,
        String resourceType,
        String resourceId,
        String detail,
        Instant occurredAt
) {
}
