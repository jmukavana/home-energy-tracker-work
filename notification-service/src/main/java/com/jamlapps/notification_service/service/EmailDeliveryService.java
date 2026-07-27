package com.jamlapps.notification_service.service;

import com.jamlapps.kafka.event.NotificationRequest;
import com.jamlapps.notification_service.entity.NotificationLog;
import com.jamlapps.notification_service.repository.NotificationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * The delivery/template concern extracted from alert-service (Phase 3). Consumes
 * {@link NotificationRequest} events produced by alert-service (and, in future,
 * any other service that needs to notify a user) rather than alert-service
 * calling SMTP directly.
 */
@Slf4j
@Service
public class EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;

    public EmailDeliveryService(JavaMailSender mailSender, NotificationLogRepository notificationLogRepository) {
        this.mailSender = mailSender;
        this.notificationLogRepository = notificationLogRepository;
    }

    @KafkaListener(topics = "notification-requests-v1", groupId = "notification-service")
    public void onNotificationRequest(NotificationRequest request) {
        log.info("Received notification request: {}", request);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getRecipientEmail());
        message.setFrom("noreply@jamlapps.com");
        message.setSubject(request.getSubject());
        message.setText(request.getBody());

        boolean sent;
        try {
            mailSender.send(message);
            sent = true;
            log.info("Email sent to: {}", request.getRecipientEmail());
        } catch (MailException e) {
            sent = false;
            log.error("Failed to send email to: {}", request.getRecipientEmail(), e);
        }

        notificationLogRepository.save(NotificationLog.builder()
                .notificationId(request.getNotificationId())
                .userId(request.getUserId())
                .recipientEmail(request.getRecipientEmail())
                .subject(request.getSubject())
                .sent(sent)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
