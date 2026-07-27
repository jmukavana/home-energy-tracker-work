package com.jamlapps.audit_service.service;

import com.jamlapps.audit_service.entity.AuditLogEntry;
import com.jamlapps.audit_service.repository.AuditLogEntryRepository;
import com.jamlapps.kafka.event.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class AuditLogListener {

    private final AuditLogEntryRepository auditLogEntryRepository;

    public AuditLogListener(AuditLogEntryRepository auditLogEntryRepository) {
        this.auditLogEntryRepository = auditLogEntryRepository;
    }

    @KafkaListener(topics = "audit-events-v1", groupId = "audit-service")
    public void onAuditEvent(AuditEvent event) {
        log.info("Recording audit event: {}", event);

        auditLogEntryRepository.save(AuditLogEntry.builder()
                .eventId(event.getEventId())
                .actorType(event.getActorType())
                .actorId(event.getActorId())
                .action(event.getAction())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .detail(event.getDetail())
                .occurredAt(Instant.ofEpochMilli(event.getOccurredAtEpochMillis()))
                .recordedAt(Instant.now())
                .build());
    }
}
