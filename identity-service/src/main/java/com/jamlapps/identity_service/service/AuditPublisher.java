package com.jamlapps.identity_service.service;

import com.jamlapps.kafka.event.AuditEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes to {@code audit-events-v1}, consumed by audit-service. This is
 * what gives audit-service a genuine producer rather than an empty topic.
 */
@Component
public class AuditPublisher {

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    public AuditPublisher(KafkaTemplate<String, AuditEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Long actorId, String action, String resourceType, String resourceId, String detail) {
        AuditEvent event = AuditEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setActorType("USER")
                .setActorId(actorId)
                .setAction(action)
                .setResourceType(resourceType)
                .setResourceId(resourceId)
                .setDetail(detail)
                .setOccurredAtEpochMillis(Instant.now().toEpochMilli())
                .build();

        kafkaTemplate.send("audit-events-v1", event);
    }
}
