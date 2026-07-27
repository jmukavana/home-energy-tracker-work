package com.jamlapps.audit_service.controller;

import com.jamlapps.audit_service.dto.AuditLogEntryDto;
import com.jamlapps.audit_service.entity.AuditLogEntry;
import com.jamlapps.audit_service.repository.AuditLogEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuditQueryController {

    private final AuditLogEntryRepository auditLogEntryRepository;

    public AuditQueryController(AuditLogEntryRepository auditLogEntryRepository) {
        this.auditLogEntryRepository = auditLogEntryRepository;
    }

    @GetMapping("/api/v1/audit/events")
    public List<AuditLogEntryDto> queryEvents(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(defaultValue = "50") int limit) {

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "occurredAt"));

        List<AuditLogEntry> entries;
        if (actorId != null) {
            entries = auditLogEntryRepository.findByActorIdOrderByOccurredAtDesc(actorId, pageable);
        } else if (resourceType != null) {
            entries = auditLogEntryRepository.findByResourceTypeOrderByOccurredAtDesc(resourceType, pageable);
        } else {
            entries = auditLogEntryRepository.findAllByOrderByOccurredAtDesc(pageable);
        }

        return entries.stream()
                .map(e -> AuditLogEntryDto.builder()
                        .id(e.getId())
                        .eventId(e.getEventId())
                        .actorType(e.getActorType())
                        .actorId(e.getActorId())
                        .action(e.getAction())
                        .resourceType(e.getResourceType())
                        .resourceId(e.getResourceId())
                        .detail(e.getDetail())
                        .occurredAt(e.getOccurredAt())
                        .build())
                .toList();
    }
}
