# AGENTS.md — Guidance for automated coding/operational agents

Purpose: Give AI agents the minimal, actionable knowledge to build, run, and inspect the Home Energy Tracker microservices.

Quick plan for agents
- Bring up infra (MySQL, Kafka, TimescaleDB, Mailpit, Kafka UI, Keycloak, Prometheus, Grafana)
- Build or run a single service locally
- Exercise the ingestion → usage → alert pipeline and verify side-effects

Quick start (infra)
- From repo root: `docker compose up -d` (uses `docker-compose.yml`)
- Stop: `docker compose down`
- If DB issues occur: remove/recreate volumes or re-run `docker/mysql/init.sql`

Build / run a service
- Build: `cd <service> && ./mvnw package` (each microservice has its own Maven wrapper)
- Run artifact: `java -jar target/<artifact>.jar` or `./mvnw spring-boot:run`

Architecture & key components
- Microservices (top-level dirs): `alert-service`, `api-gateway`, `device-service`, `ingestion-service`, `insight-service`, `usage-service`, `user-service`, plus 4 new WattWise Phase 3 bounded contexts: `notification-service`, `identity-service`, `audit-service`, `billing-service`.
- Stack: **Spring Boot 4** + **Java 21** for most services; **`insight-service`** uses **Spring Boot 3.5** with **Spring AI** (Ollama). **Spring Cloud 2025.1.0** on the gateway (Gateway Server WebMVC, Resilience4j circuit breakers). No Spring Cloud Config Server in this repo—config is per-service `application.properties`.
- Important infra files: `docker-compose.yml`, `docker/mysql/init.sql`, `docker/prometheus/prometheus.yml`, `docker/grafana/provisioning/`, `docker/kafka_data/`.
- Human-oriented architecture diagrams: `diagrams/*.png` (see top-level `README.md`).

Critical integration points & dataflows (explicit)
- Ingestion → Kafka → Usage → TimescaleDB & Alerts → Alerting consumer
  - Topic `energy-usage-v2` (Avro, via Schema Registry): produced by `ingestion-service` (`ingestion-service/src/main/java/com/jamlapps/ingestion_service/service/IngestionService.java`) and consumed by `usage-service`'s two CQRS listeners, `command/UsageIngestionListener.java` (writes the TimescaleDB `energy_usage` hypertable via `command/TimescaleEnergyUsageWriter.java`) and `query/UsageProjectionListener.java` (writes the MySQL `device_usage_summary` read model, with idempotency dedup via `processed_usage_events`).
  - Topic `energy-alerts-v2` (Avro): produced by `usage-service/src/main/java/com/jamlapps/usage_service/alerting/UsageAlertingScheduler.java` (1-hour rolling threshold check, now a SQL query against TimescaleDB) and consumed by `alert-service` (`alert-service/src/main/java/com/jamlapps/alert_service/service/AlertService.java`).
  - Both topics were migrated from plain JSON (`energy-usage`/`energy-alerts`) to Avro + Confluent Schema Registry; the `-v2` suffix is permanent (Kafka topics can't be renamed in place), the old JSON topics were decommissioned.
- **Phase 3 (new bounded contexts)**: `alert-service` no longer sends email or persists anything itself — it consumes `energy-alerts-v2`, builds a `NotificationRequest`, and produces to topic `notification-requests-v1`, which `notification-service` (port **8087**) consumes to actually send via Mailpit and log delivery (`GET /api/v1/notifications/user/{userId}`). `identity-service` (port **8088**) manages API keys (`POST/GET/DELETE /api/v1/identity/api-keys...`) and publishes `AuditEvent`s to topic `audit-events-v1` on key create/revoke. `audit-service` (port **8089**) consumes that topic into an insert-only audit log (`GET /api/v1/audit/events`). `billing-service` (port **8090**) calls `usage-service`'s `GET /api/v1/usage/{userId}` to compute usage-based invoices at a flat rate (`POST /api/v1/billing/invoices/generate/{userId}`); real Stripe SDK integration exists but is disabled by default (`billing.stripe.enabled=false` — no live Stripe test key in this environment). Each of these 4 new services owns its **own** MySQL database (not `home_energy_tracker`).
- **TimescaleDB is the live time-series store** (InfluxDB was decommissioned in Phase 2c of the WattWise migration): `usage-service/src/main/java/com/jamlapps/usage_service/config/DataSourceConfig.java` wires two Postgres connections — `timescaleDataSource` (postgres superuser, used only by `TimescaleMigrationRunner` for Flyway DDL) and `timescaleAppDataSource`/`timescaleAppJdbcTemplate` (restricted `usage_app` role, created by `db/timescale/V2__app_role.sql`, actually subject to the row-level-security policy on `energy_usage` since superusers bypass RLS). Schema: `db/timescale/V1__energy_usage_hypertable.sql` (hypertable, `tenant_id` default 1, RLS enabled — compression was deliberately dropped since TimescaleDB doesn't allow compression and RLS on the same hypertable).
- MySQL: DB name `home_energy_tracker`, init in `docker/mysql/init.sql`; JDBC URLs appear in services' `src/main/resources/application.properties`. Keycloak uses a separate MySQL instance in Compose (`keycloak-db`).

API Gateway (routing & security)
- Gateway port **9000**; routes under `/api/v1/...` proxy to localhost backends with **circuit breakers** (see `api-gateway/src/main/java/com/jamlapps/api_gateway/route/*.java`).
- **OAuth2 Resource Server** (JWT): most paths require a valid token; **permitAll** list is in `api-gateway/src/main/resources/application.properties` (`security.excluded.urls`—actuator, swagger, api-docs). **`/api/v1/ingestion/**` is not excluded**—calls through the gateway typically need a **Bearer** token from Keycloak (`http://localhost:8091`). For quick unauthenticated pipeline tests, **POST directly to ingestion-service on port 8082** (service has no Spring Security).

Observability & useful endpoints
- Kafka UI: http://localhost:8070 (inspect topics and messages; Schema Registry-aware, so Avro payloads on `energy-usage-v2`/`energy-alerts-v2` render decoded, not as raw bytes)
- Schema Registry: http://localhost:8086 (REST API, e.g. `GET /subjects`; container listens internally on 8081, which collides with `device-service`'s host port, hence the 8086 host mapping)
- Mailpit (SMTP/web): SMTP **1025**, web UI http://localhost:8025 (outgoing email from `alert-service`)
- TimescaleDB (Postgres): localhost:5433 (host port; 5432 collides with a host-level PostgreSQL service already running on this machine), db `usage_timeseries`. Admin/migration connection is `postgres`/`password` (superuser, Flyway only); the app connects as the restricted `usage_app`/`usage_app_password` role so RLS actually applies. This is now the live time-series store — InfluxDB was decommissioned in Phase 2c.
- Keycloak: http://localhost:8091 (admin / admin per Compose; realm import under `docker/keycloak/realms/`)
- Prometheus: http://localhost:9090 — scrapes Spring apps at `/actuator/prometheus` on **host** via `host.docker.internal` (see `docker/prometheus/prometheus.yml`). Run microservices **on the host** with default ports for metrics to appear in Prometheus.
- Grafana: http://localhost:3000 (admin / admin)
- Service ports (defaults in `application.properties`):
  - `user-service` **8080**, `device-service` **8081**, `ingestion-service` **8082**, `usage-service` **8083**, `alert-service` **8084**, `insight-service` **8085**, `api-gateway` **9000**
  - Phase 3: `notification-service` **8087**, `identity-service` **8088**, `audit-service` **8089**, `billing-service` **8090**

Example agent actions (curl + checks)
- Post a test event (Kafka traffic; **direct to ingestion**, no JWT):
  - `curl -X POST http://localhost:8082/api/v1/ingestion -H 'Content-Type: application/json' -d '{"deviceId":1,"energyConsumed":1200,"timestamp":"2026-01-01T12:00:00Z"}'`
- Through the **gateway** (requires JWT): `http://localhost:9000/api/v1/ingestion` with `Authorization: Bearer <token>`.
- Verify `usage-service` consumed and wrote to TimescaleDB: check `usage-service` logs and query the hypertable directly, e.g. `docker exec timescaledb psql -U postgres -d usage_timeseries -c "SELECT * FROM energy_usage ORDER BY time DESC LIMIT 5;"`; also check scheduled aggregation logs (scheduler in `UsageAlertingScheduler.java`).
- Force alert: craft a high `energyConsumed` payload and confirm Mailpit received an email (web UI at **8025**).

Agent runbook checks (short)
- Confirm ports reachable: **3306** (MySQL), **9094** (Kafka external), **5433** (TimescaleDB), **8025** (Mailpit), **8070** (Kafka UI), **8086** (Schema Registry), **8091** (Keycloak), **9090** (Prometheus), **3000** (Grafana)
- Confirm Kafka topics exist and show message flow (Kafka UI or service logs)
- Confirm TimescaleDB writes: `docker exec timescaledb psql -U postgres -d usage_timeseries -c "SELECT count(*) FROM energy_usage;"`
- Check service logs: `docker compose logs <container>` or run the JAR locally and capture stdout

Project-specific conventions
- Maven wrapper present in each service — prefer `./mvnw`.
- Package names use underscores: e.g. `com.jamlapps.ingestion_service` (see each service `HELP.md` which documents this change).
- Kafka bootstrap server for **host-based** service runs: **`localhost:9094`** (external advertised listener in `docker-compose.yml`). Services' `application.properties` should use this when not on the Docker network.
- Kafka events (`EnergyUsageEvent`, `AlertingEvent`) are Avro, generated per-service from `.avsc` files under `src/main/avro/` via `avro-maven-plugin` (schema validated/stored in Confluent Schema Registry at `http://localhost:8086`) — not hand-written POJOs and not plain JSON. Generated classes use `newBuilder()`/getters (`SpecificRecord`), not Lombok builders/records.
- **Flyway migration ownership**: all MySQL-backed *legacy* services share one physical schema (`home_energy_tracker`), so each service that owns Flyway migrations against it must set its own distinct `spring.flyway.table` history table name (e.g. `alert-service` uses `flyway_schema_history_alert`, `usage-service` uses `flyway_schema_history_usage`) — otherwise two services' independent version-1 migrations collide against the same default-named history table and whichever starts second fails checksum validation. `user-service` is the exception: as the original/default owner it keeps Flyway's default table name. `device-service` still doesn't own its own migration (`device` table is migrated by `user-service`'s `V2__device_table.sql`) because that table has a live FK to `user` — splitting it would create a real cross-service startup-ordering dependency; this is deliberately deferred, not an oversight. The 4 Phase 3 services deliberately do **not** follow this pattern — each has its own dedicated database (`notification_service`, `identity_service`, `audit_service`, `billing_service` in `docker/mysql/init.sql`), so Flyway's default history table name is fine for all of them.

Files to reference when automating (examples)
- `docker-compose.yml` — infra and envs
- `docker/mysql/init.sql` — DB bootstrap
- `docker/prometheus/prometheus.yml` — Prometheus scrape targets
- `ingestion-service/src/main/java/com/jamlapps/ingestion_service/controller/IngestionController.java`
- `ingestion-service/src/main/java/com/jamlapps/ingestion_service/service/IngestionService.java`
- `usage-service/src/main/java/com/jamlapps/usage_service/command/UsageIngestionListener.java`
- `usage-service/src/main/java/com/jamlapps/usage_service/query/UsageProjectionListener.java`
- `usage-service/src/main/java/com/jamlapps/usage_service/alerting/UsageAlertingScheduler.java`
- `usage-service/src/main/java/com/jamlapps/usage_service/config/DataSourceConfig.java`
- `usage-service/src/main/java/com/jamlapps/usage_service/command/TimescaleEnergyUsageWriter.java`
- `alert-service/src/main/java/com/jamlapps/alert_service/service/AlertService.java`
- `api-gateway/src/main/java/com/jamlapps/api_gateway/route/UserServiceRoutes.java` (and sibling route classes)
- `api-gateway/src/main/java/com/jamlapps/api_gateway/config/SecurityConfig.java`

Notes
- Java version: use **JDK 21** (declared in each module POM).
- The top-level `README.md` and each service `HELP.md` contain additional module-specific hints.

End of AGENTS.md
