package com.jamlapps.usage_service.query;

import com.jamlapps.usage_service.client.DeviceClient;
import com.jamlapps.usage_service.dto.DeviceDto;
import com.jamlapps.usage_service.dto.UsageDto;
import com.jamlapps.usage_service.query.entity.DeviceUsageSummary;
import com.jamlapps.usage_service.query.repository.DeviceUsageSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageQueryServiceTest {

    @Mock
    private DeviceClient deviceClient;

    @Mock
    private DeviceUsageSummaryRepository repository;

    @Test
    void getXDaysUsageForUser_sumsPerDeviceAcrossDates() {
        UsageQueryService service = new UsageQueryService(deviceClient, repository);

        Long userId = 42L;
        DeviceDto deviceA = DeviceDto.builder().id(1L).name("Fridge").userId(userId).build();
        DeviceDto deviceB = DeviceDto.builder().id(2L).name("Heater").userId(userId).build();

        when(deviceClient.getAllDevicesForUser(userId)).thenReturn(List.of(deviceA, deviceB));
        when(repository.findByDeviceIdInAndUsageDateBetween(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(
                DeviceUsageSummary.builder().deviceId(1L).usageDate(LocalDate.now()).energyConsumed(3.0).build(),
                DeviceUsageSummary.builder().deviceId(1L).usageDate(LocalDate.now().minusDays(1)).energyConsumed(2.0).build(),
                DeviceUsageSummary.builder().deviceId(2L).usageDate(LocalDate.now()).energyConsumed(7.5).build()
        ));

        UsageDto result = service.getXDaysUsageForUser(userId, 3);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.devices()).hasSize(2);
        assertThat(result.devices().stream().filter(d -> d.id().equals(1L)).findFirst().orElseThrow().energyConsumed())
                .isEqualTo(5.0);
        assertThat(result.devices().stream().filter(d -> d.id().equals(2L)).findFirst().orElseThrow().energyConsumed())
                .isEqualTo(7.5);
    }

    @Test
    void getXDaysUsageForUser_noDevices_returnsEmptyUsageDto() {
        UsageQueryService service = new UsageQueryService(deviceClient, repository);

        when(deviceClient.getAllDevicesForUser(42L)).thenReturn(List.of());

        UsageDto result = service.getXDaysUsageForUser(42L, 3);

        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.devices()).isNull();
    }
}
