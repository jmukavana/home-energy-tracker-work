package com.jamlapps.usage_service.client;

import com.jamlapps.usage_service.exception.DeviceServiceUnavailableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceClientFallbackTest {

    private final DeviceClient deviceClient = new DeviceClient("http://localhost:8081/api/v1/device");

    @Test
    void getDeviceByIdFallback_returnsNull() {
        Long deviceId = 42L;

        var result = deviceClient.getDeviceByIdFallback(deviceId, new RuntimeException("boom"));

        assertThat(result).isNull();
    }

    @Test
    void getAllDevicesForUserFallback_throwsDeviceServiceUnavailableException() {
        Long userId = 7L;

        assertThatThrownBy(() -> deviceClient.getAllDevicesForUserFallback(userId, new RuntimeException("boom")))
                .isInstanceOf(DeviceServiceUnavailableException.class)
                .hasMessageContaining("7");
    }
}
