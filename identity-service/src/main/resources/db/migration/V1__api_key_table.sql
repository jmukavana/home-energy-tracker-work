CREATE TABLE `api_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `label` VARCHAR(255) NOT NULL,
  `hashed_key` CHAR(64) NOT NULL,
  `key_prefix` VARCHAR(20) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked_at` TIMESTAMP NULL,
  `last_used_at` TIMESTAMP NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_key_hashed_key` (`hashed_key`),
  KEY `idx_api_key_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
