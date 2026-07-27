package com.jamlapps.usage_service.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Phase 2b dual-write target: inserts into the {@code energy_usage} hypertable
 * (Phase 2a) alongside {@link UsageIngestionListener}'s existing InfluxDB write.
 * {@code tenant_id} is omitted so the column's {@code DEFAULT 1} applies, matching
 * Phase 4's single-seed-tenant backfill decision.
 *
 * <p>Upserts on {@code (time, device_id)} rather than a plain insert: the event's
 * {@code timestampEpochMillis} field is millisecond-precision only (a deliberate
 * Phase 1b simplification), so two events for the same device within the same
 * millisecond are a real occurrence under load, not an edge case. InfluxDB's point
 * model already resolves that case by last-write-wins (a second point at an
 * identical measurement/tags/timestamp overwrites the field value) — the upsert
 * here reproduces that exact semantics so the two stores stay comparable for parity.
 */
@Component
@Slf4j
public class TimescaleEnergyUsageWriter {

    private final JdbcTemplate timescaleAppJdbcTemplate;

    public TimescaleEnergyUsageWriter(@Qualifier("timescaleAppJdbcTemplate") JdbcTemplate timescaleAppJdbcTemplate) {
        this.timescaleAppJdbcTemplate = timescaleAppJdbcTemplate;
    }

    public void write(Long deviceId, Double energyConsumed, Instant time) {
        timescaleAppJdbcTemplate.update(
                """
                INSERT INTO energy_usage (time, device_id, energy_consumed) VALUES (?, ?, ?)
                ON CONFLICT (time, device_id) DO UPDATE SET energy_consumed = EXCLUDED.energy_consumed
                """,
                Timestamp.from(time), deviceId, energyConsumed);
    }
}
