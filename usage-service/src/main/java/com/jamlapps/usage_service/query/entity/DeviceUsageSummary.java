package com.jamlapps.usage_service.query.entity;

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
import java.time.LocalDate;

@Entity
@Table(name = "device_usage_summary")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DeviceUsageSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deviceId;
    private Long userId;
    private LocalDate usageDate;
    private double energyConsumed;
    private Instant updatedAt;
}
