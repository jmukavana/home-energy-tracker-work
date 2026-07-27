package com.jamlapps.notification_service.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationLogDto(
        Long id,
        String notificationId,
        Long userId,
        String recipientEmail,
        String subject,
        boolean sent,
        LocalDateTime createdAt
) {
}
