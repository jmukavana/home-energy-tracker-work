CREATE TABLE `invoice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `total_kwh` DOUBLE NOT NULL,
  `amount_cents` BIGINT NOT NULL,
  `currency` VARCHAR(3) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `stripe_invoice_id` VARCHAR(255),
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_invoice_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
