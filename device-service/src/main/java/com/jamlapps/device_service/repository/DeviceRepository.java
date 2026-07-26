package com.jamlapps.device_service.repository;

import com.jamlapps.device_service.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findAllByUserId(Long userId);
}
