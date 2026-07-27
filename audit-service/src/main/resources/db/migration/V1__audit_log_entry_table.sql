CREATE TABLE `audit_log_entry` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `event_id` VARCHAR(36) NOT NULL,
  `actor_type` VARCHAR(50) NOT NULL,
  `actor_id` BIGINT NOT NULL,
  `action` VARCHAR(100) NOT NULL,
  `resource_type` VARCHAR(100) NOT NULL,
  `resource_id` VARCHAR(100) NOT NULL,
  `detail` VARCHAR(1000),
  `occurred_at` TIMESTAMP(3) NOT NULL,
  `recorded_at` TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_log_entry_event_id` (`event_id`),
  KEY `idx_audit_log_entry_actor_id` (`actor_id`),
  KEY `idx_audit_log_entry_resource_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
