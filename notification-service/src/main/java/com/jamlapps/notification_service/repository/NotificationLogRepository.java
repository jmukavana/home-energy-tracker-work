package com.jamlapps.notification_service.repository;

import com.jamlapps.notification_service.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
