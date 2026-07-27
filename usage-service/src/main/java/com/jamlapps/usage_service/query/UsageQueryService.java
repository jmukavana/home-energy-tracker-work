package com.jamlapps.usage_service.query;

import com.jamlapps.usage_service.client.DeviceClient;
import com.jamlapps.usage_service.dto.DeviceDto;
import com.jamlapps.usage_service.dto.UsageDto;
import com.jamlapps.usage_service.query.entity.DeviceUsageSummary;
import com.jamlapps.usage_service.query.repository.DeviceUsageSummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Query side of the CQRS split: reads only from the {@code device_usage_summary}
 * read model maintained by {@link UsageProjectionListener} — no InfluxDB calls
 * on the request path at all.
 */
@Service
@Slf4j
public class UsageQueryService {

    private final DeviceClient deviceClient;
    private final DeviceUsageSummaryRepository repository;

    public UsageQueryService(DeviceClient deviceClient, DeviceUsageSummaryRepository repository) {
        this.deviceClient = deviceClient;
        this.repository = repository;
    }

    public UsageDto getXDaysUsageForUser(Long userId, int days) {
        log.info("Getting usage for userId {} over past {} days", userId, days);
        final List<DeviceDto> devicesDto = deviceClient.getAllDevicesForUser(userId);

        if (devicesDto == null || devicesDto.isEmpty()) {
            return UsageDto.builder()
                    .userId(userId)
                    .devices(null)
                    .build();
        }

        final List<Long> deviceIds = devicesDto.stream()
                .map(DeviceDto::id)
                .filter(java.util.Objects::nonNull)
                .toList();

        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final List<DeviceUsageSummary> rows =
                repository.findByDeviceIdInAndUsageDateBetween(deviceIds, today.minusDays(days), today);

        final Map<Long, Double> sumByDevice = rows.stream()
                .collect(Collectors.groupingBy(
                        DeviceUsageSummary::getDeviceId,
                        Collectors.summingDouble(DeviceUsageSummary::getEnergyConsumed)));

        log.info("Aggregated energy consumption for userId {}: {}", userId, sumByDevice);

        final List<DeviceDto> resultDevices = devicesDto.stream()
                .map(d -> DeviceDto.builder()
                        .id(d.id())
                        .name(d.name())
                        .type(d.type())
                        .location(d.location())
                        .userId(d.userId())
                        .energyConsumed(sumByDevice.getOrDefault(d.id(), 0.0))
                        .build())
                .toList();

        return UsageDto.builder()
                .userId(userId)
                .devices(resultDevices)
                .build();
    }
}
