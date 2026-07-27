package com.jamlapps.notification_service.controller;

import com.jamlapps.notification_service.dto.NotificationLogDto;
import com.jamlapps.notification_service.repository.NotificationLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationController(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @GetMapping("/user/{userId}")
    public List<NotificationLogDto> getDeliveryHistory(@PathVariable Long userId) {
        return notificationLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(log -> NotificationLogDto.builder()
                        .id(log.getId())
                        .notificationId(log.getNotificationId())
                        .userId(log.getUserId())
                        .recipientEmail(log.getRecipientEmail())
                        .subject(log.getSubject())
                        .sent(log.isSent())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }
}
