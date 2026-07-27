package com.jamlapps.usage_service.query;

import com.jamlapps.kafka.event.EnergyUsageEvent;
import com.jamlapps.usage_service.client.DeviceClient;
import com.jamlapps.usage_service.dto.DeviceDto;
import com.jamlapps.usage_service.query.repository.DeviceUsageSummaryRepository;
import com.jamlapps.usage_service.query.repository.ProcessedUsageEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Read side of the CQRS split. Consumes the same {@code energy-usage-v2} topic
 * as {@link com.jamlapps.usage_service.command.UsageIngestionListener} but
 * under a different consumer group ("usage-service-read-model"), so Kafka
 * delivers every event to both independently. Projects each event into a
 * denormalized, query-optimized MySQL summary table that
 * {@link UsageQueryService} reads from directly, instead of the API scanning
 * raw InfluxDB points per request.
 *
 * Idempotency: {@code eventId} is checked against {@code processed_usage_events}
 * before the upsert, inside the same transaction, so a Kafka redelivery of the
 * same message is skipped rather than double-counted.
 *
 * Known limitations (documented rather than solved):
 * - Eventual consistency: this listener and the InfluxDB write commit
 *   independently, so a read immediately after a write may lag briefly.
 * - userId is resolved with a synchronous per-event REST call to
 *   device-service, mirroring the existing UsageAlertingScheduler pattern. In
 *   production this would be avoided by including userId directly in the
 *   EnergyUsageEvent payload.
 */
@Component
@Slf4j
public class UsageProjectionListener {

    private final DeviceUsageSummaryRepository repository;
    private final ProcessedUsageEventRepository processedUsageEventRepository;
    private final DeviceClient deviceClient;

    public UsageProjectionListener(DeviceUsageSummaryRepository repository,
                                    ProcessedUsageEventRepository processedUsageEventRepository,
                                    DeviceClient deviceClient) {
        this.repository = repository;
        this.processedUsageEventRepository = processedUsageEventRepository;
        this.deviceClient = deviceClient;
    }

    @Transactional
    @KafkaListener(topics = "energy-usage-v2", groupId = "usage-service-read-model")
    public void onEnergyUsageEvent(EnergyUsageEvent event) {
        if (processedUsageEventRepository.markProcessed(event.getEventId()) == 0) {
            log.info("Skipping already-processed eventId {}", event.getEventId());
            return;
        }

        Long userId = null;
        try {
            DeviceDto device = deviceClient.getDeviceById(event.getDeviceId());
            userId = device != null ? device.userId() : null;
        } catch (Exception e) {
            log.warn("Could not resolve userId for device {}, projecting with null userId", event.getDeviceId());
        }

        LocalDate usageDate = Instant.ofEpochMilli(event.getTimestampEpochMillis()).atZone(ZoneOffset.UTC).toLocalDate();
        repository.upsertIncrement(event.getDeviceId(), userId, usageDate, event.getEnergyConsumed());
    }
}
