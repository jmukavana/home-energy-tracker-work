package com.jamlapps.notification_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Delivery-tracking record — the bounded-context responsibility this service
 * took over from alert-service's old {@code Alert} entity/table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String notificationId;
    private Long userId;
    private String recipientEmail;
    private String subject;
    private boolean sent;
    private LocalDateTime createdAt;
}
