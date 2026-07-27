package com.jamlapps.alert_service.service;

import com.jamlapps.kafka.event.AlertingEvent;
import com.jamlapps.kafka.event.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Rule-triggered-alert consumer. Phase 3 extracted the actual delivery/template
 * concern into {@code notification-service} — this class now only decides
 * *that* a notification is needed and builds its content; it no longer talks
 * to SMTP or persists anything itself (see {@code notification-requests-v1}).
 */
@Slf4j
@Service
public class AlertService {

    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    public AlertService(KafkaTemplate<String, NotificationRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "energy-alerts-v2", groupId = "alert-service")
    public void energyUsageAlertEvent(AlertingEvent alertingEvent) {
        log.info("Received alert event: {}", alertingEvent);

        final String subject = "Energy Usage Alert for User " + alertingEvent.getUserId();
        final String body = "Alert: " + alertingEvent.getMessage() +
                "\nThreshold: " + alertingEvent.getThreshold() +
                "\nEnergy Consumed: " + alertingEvent.getEnergyConsumed();

        final NotificationRequest request = NotificationRequest.newBuilder()
                .setNotificationId(UUID.randomUUID().toString())
                .setUserId(alertingEvent.getUserId())
                .setRecipientEmail(alertingEvent.getEmail())
                .setSubject(subject)
                .setBody(body)
                .build();

        kafkaTemplate.send("notification-requests-v1", request);
    }
}
