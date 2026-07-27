package com.jamlapps.usage_service.query.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_usage_events")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ProcessedUsageEvent {

    @Id
    private String eventId;

    private Instant processedAt;
}
