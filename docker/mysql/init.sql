CREATE DATABASE IF NOT EXISTS home_energy_tracker;
USE home_energy_tracker;

-- Phase 3: each new bounded-context service gets its own database rather than
-- sharing home_energy_tracker (avoids reproducing the cross-service
-- Flyway/schema-ownership mess Phase 0 had to untangle for the legacy 7).
CREATE DATABASE IF NOT EXISTS notification_service;
CREATE DATABASE IF NOT EXISTS identity_service;
CREATE DATABASE IF NOT EXISTS audit_service;
CREATE DATABASE IF NOT EXISTS billing_service;