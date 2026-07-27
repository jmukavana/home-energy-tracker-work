package com.jamlapps.usage_service.command;

import com.jamlapps.kafka.event.EnergyUsageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Write side of the CQRS split: the source of truth for raw telemetry.
 * Appends every {@link EnergyUsageEvent} to the TimescaleDB {@code energy_usage}
 * hypertable, independently of {@link com.jamlapps.usage_service.query.UsageProjectionListener},
 * which consumes the same topic under a different consumer group to maintain the
 * read model.
 *
 * <p>Phase 2c: cut over from InfluxDB (Phase 2a/2b's dual-write target) to
 * TimescaleDB as the sole raw-telemetry store. Unlike the Phase 2b dual-write,
 * a write failure here is allowed to propagate — this is now the only copy of
 * the data, so Kafka's at-least-once redelivery should retry a genuine failure
 * rather than silently dropping it.
 */
@Component
@Slf4j
public class UsageIngestionListener {

    private final TimescaleEnergyUsageWriter timescaleEnergyUsageWriter;

    public UsageIngestionListener(TimescaleEnergyUsageWriter timescaleEnergyUsageWriter) {
        this.timescaleEnergyUsageWriter = timescaleEnergyUsageWriter;
    }

    @KafkaListener(topics = "energy-usage-v2", groupId = "usage-service")
    public void energyUsageEvent(EnergyUsageEvent energyUsageEvent) {
        Instant time = Instant.ofEpochMilli(energyUsageEvent.getTimestampEpochMillis());
        timescaleEnergyUsageWriter.write(energyUsageEvent.getDeviceId(), energyUsageEvent.getEnergyConsumed(), time);
    }
}
