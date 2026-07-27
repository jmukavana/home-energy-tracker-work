CREATE TABLE `notification_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `notification_id` VARCHAR(36) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `recipient_email` VARCHAR(255) NOT NULL,
  `subject` VARCHAR(255) NOT NULL,
  `sent` BOOLEAN NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notification_log_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
