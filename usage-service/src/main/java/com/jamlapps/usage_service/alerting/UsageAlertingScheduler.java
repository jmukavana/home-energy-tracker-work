package com.jamlapps.usage_service.alerting;

import com.jamlapps.kafka.event.AlertingEvent;
import com.jamlapps.usage_service.client.DeviceClient;
import com.jamlapps.usage_service.client.UserClient;
import com.jamlapps.usage_service.dto.DeviceDto;
import com.jamlapps.usage_service.dto.UserDto;
import com.jamlapps.usage_service.model.DeviceEnergy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Threshold-alerting concern, deliberately kept separate from the CQRS
 * command/query split: this checks a rolling 1-hour window (different
 * granularity/purpose than the daily read model in the {@code query}
 * package) and is untouched behaviorally by that refactor.
 *
 * <p>Phase 2c: reads from the TimescaleDB {@code energy_usage} hypertable instead
 * of InfluxDB — same trailing-1-hour-grouped-by-device aggregation, now a plain
 * SQL query via the RLS-respecting {@code timescaleAppJdbcTemplate} instead of Flux.
 */
@Component
@Slf4j
public class UsageAlertingScheduler {

    private final JdbcTemplate timescaleAppJdbcTemplate;
    private final DeviceClient deviceClient;
    private final UserClient userClient;
    private final KafkaTemplate<String, AlertingEvent> kafkaTemplate;

    public UsageAlertingScheduler(@Qualifier("timescaleAppJdbcTemplate") JdbcTemplate timescaleAppJdbcTemplate,
                                   DeviceClient deviceClient,
                                   UserClient userClient,
                                   KafkaTemplate<String, AlertingEvent> kafkaTemplate) {
        this.timescaleAppJdbcTemplate = timescaleAppJdbcTemplate;
        this.deviceClient = deviceClient;
        this.userClient = userClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void aggregateDeviceEnergyUsage() {
        final Instant now = Instant.now();
        final Instant oneHourAgo = now.minusSeconds(3600);

        List<DeviceEnergy> deviceEnergies = timescaleAppJdbcTemplate.query(
                """
                SELECT device_id, SUM(energy_consumed) AS total
                FROM energy_usage
                WHERE time >= ? AND time < ?
                GROUP BY device_id
                """,
                (rs, rowNum) -> DeviceEnergy.builder()
                        .deviceId(rs.getLong("device_id"))
                        .energyConsumed(rs.getDouble("total"))
                        .build(),
                Timestamp.from(oneHourAgo), Timestamp.from(now));

        log.info("Aggregated device energies over the past hour: {}", deviceEnergies);

        for (DeviceEnergy deviceEnergy : deviceEnergies) {
            try {
                final DeviceDto deviceResponse = deviceClient.getDeviceById(deviceEnergy.getDeviceId());

                if (deviceResponse == null || deviceResponse.id() == null) {
                    log.warn("Device not found for ID: {}", deviceEnergy.getDeviceId());
                    continue;
                }
                deviceEnergy.setUserId(deviceResponse.userId());
            } catch (Exception e) {
                log.warn("Failed to fetch device for ID: {}", deviceEnergy.getDeviceId());
            }
        }

        // remove devices with null userId
        deviceEnergies.removeIf(de -> de.getUserId() == null);

        // Get user-device mapping and aggregate per user
        Map<Long, List<DeviceEnergy>> userDeviceEnergyMap =
                deviceEnergies.stream()
                        .collect(Collectors.groupingBy(DeviceEnergy::getUserId));

        log.info("User-Device Energy Map: {}", userDeviceEnergyMap);

        // get users energy consumption thresholds
        List<Long> userIds = new ArrayList<>(userDeviceEnergyMap.keySet());
        final Map<Long, Double> userThresholdMap = new HashMap<>();
        final Map<Long, String> userEmailMap = new HashMap<>();

        for (final Long userId : userIds) {
            try {
                UserDto user = userClient.getUserById(userId);
                if (user == null || user.id() == null || !user.alerting()) {
                    log.warn("User not found or alerting disabled for ID: {}", userId);
                    continue;
                }
                userThresholdMap.put(userId, user.energyAlertingThreshold());
                userEmailMap.put(userId, user.email());
            } catch (Exception e) {
                log.warn("Failed to fetch user for ID: {}", userId);
            }
        }
        log.info("User Threshold Map: {}", userThresholdMap);


        // Check thresholds against aggregated usage
        final List<Long> alertedUsers = new ArrayList<>(userThresholdMap.keySet());
        for (final Long userId : alertedUsers) {
            final Double threshold = userThresholdMap.get(userId);
            final List<DeviceEnergy> devices = userDeviceEnergyMap.get(userId);

            final Double totalConsumption = devices.stream()
                    .mapToDouble(DeviceEnergy::getEnergyConsumed)
                    .sum();

           if (totalConsumption > threshold) {
               log.info("ALERT: User ID {} has exceeded the energy threshold! " +
                       "Total Consumption: {}, Threshold: {}",
                       userId, totalConsumption, threshold);
               // Put message on kafka alert-topic
               final AlertingEvent alertingEvent = AlertingEvent.newBuilder()
                       .setUserId(userId)
                       .setMessage("Energy consumption threshold exceeded")
                       .setThreshold(threshold)
                       .setEnergyConsumed(totalConsumption)
                       .setEmail(userEmailMap.get(userId))
                       .build();
               kafkaTemplate.send("energy-alerts-v2", alertingEvent);
           } else {
                log.info("User ID {} is within the energy threshold. " +
                          "Total Consumption: {}, Threshold: {}",
                          userId, totalConsumption, threshold);
           }
        }
    }
}
