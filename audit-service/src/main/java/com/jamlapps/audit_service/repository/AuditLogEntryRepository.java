package com.jamlapps.audit_service.repository;

import com.jamlapps.audit_service.entity.AuditLogEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Deliberately exposes no update/delete methods — immutability is enforced by
 * this repository's shape, not by a database trigger.
 */
public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, Long> {
    List<AuditLogEntry> findByActorIdOrderByOccurredAtDesc(Long actorId, Pageable pageable);
    List<AuditLogEntry> findByResourceTypeOrderByOccurredAtDesc(String resourceType, Pageable pageable);
    List<AuditLogEntry> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
