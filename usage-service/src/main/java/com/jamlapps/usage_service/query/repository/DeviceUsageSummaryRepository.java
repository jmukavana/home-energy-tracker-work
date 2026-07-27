package com.jamlapps.usage_service.query.repository;

import com.jamlapps.usage_service.query.entity.DeviceUsageSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeviceUsageSummaryRepository extends JpaRepository<DeviceUsageSummary, Long> {

    List<DeviceUsageSummary> findByDeviceIdInAndUsageDateBetween(
            List<Long> deviceIds, LocalDate start, LocalDate end);

    @Modifying
    @Query(value = """
        INSERT INTO device_usage_summary (device_id, user_id, usage_date, energy_consumed, updated_at)
        VALUES (:deviceId, :userId, :usageDate, :delta, CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE
            energy_consumed = energy_consumed + VALUES(energy_consumed),
            user_id = VALUES(user_id),
            updated_at = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void upsertIncrement(@Param("deviceId") Long deviceId,
                          @Param("userId") Long userId,
                          @Param("usageDate") LocalDate usageDate,
                          @Param("delta") double delta);
}
