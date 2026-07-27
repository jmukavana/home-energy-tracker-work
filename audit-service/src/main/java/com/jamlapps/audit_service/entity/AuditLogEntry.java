package com.jamlapps.audit_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Immutable by convention: no update/delete method exists anywhere in this
 * service's repository or service layer, only inserts via the Kafka listener.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_log_entry")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventId;
    private String actorType;
    private Long actorId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private Instant occurredAt;
    private Instant recordedAt;
}
