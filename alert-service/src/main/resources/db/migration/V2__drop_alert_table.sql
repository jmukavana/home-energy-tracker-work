-- Phase 3: delivery tracking moved to notification-service's own `notification_log`
-- table (its own database) — alert-service no longer persists anything itself.
DROP TABLE IF EXISTS `alert`;
