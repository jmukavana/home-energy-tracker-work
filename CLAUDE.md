# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Home Energy Tracker is a **microservices reference implementation** for monitoring household electricity usage: devices report power readings, an ingestion service publishes them to Kafka, a usage service aggregates and stores them in TimescaleDB, and an alert service emails users when thresholds are crossed. All public traffic is fronted by a single API Gateway with JWT auth and circuit breakers.

See `README.md` for the full narrative/architecture write-up and `AGENTS.md` for an operator-focused runbook (curl examples, port list, troubleshooting) — both are kept up to date and worth reading alongside this file.

## Repository layout

Seven independent Maven/Spring Boot projects, each with its own `pom.xml` and `mvnw` wrapper — there is no root/parent build or reactor pom, so all builds and tests are run per-service from inside that service's directory:

- `api-gateway` (port 9000) — public entry point
- `user-service` (8080), `device-service` (8081) — domain/CRUD services backed by MySQL
- `ingestion-service` (8082) — HTTP intake, publishes to Kafka
- `usage-service` (8083) — Kafka consumer/producer, writes to TimescaleDB, evaluates alert thresholds
- `alert-service` (8084) — Kafka consumer, sends email
- `insight-service` (8085) — LLM-backed usage insights via Spring AI/Ollama

## Commands

### Infra (Docker Compose, from repo root)

```bash
docker compose up -d      # MySQL, Kafka, TimescaleDB, Mailpit, Kafka UI, Keycloak, Prometheus, Grafana
docker compose down
```

If MySQL fails to initialize, remove the volume or re-run `docker/mysql/init.sql`.

### Build / run a single service

```bash
cd <service> && ./mvnw package          # build
./mvnw spring-boot:run                  # run from source
java -jar target/<service>-*.jar        # run built artifact
```

### Tests

```bash
./mvnw test                                     # all tests in a service
./mvnw test -Dtest=ClassName                    # single test class
./mvnw test -Dtest=ClassName#methodName         # single test method
```

Only `user-service` has integration tests (`src/test/java/.../integration/UserServiceIntegrationTest.java`); they use Testcontainers (`@Testcontainers(disabledWithoutDocker = true)`, `MySqlTestcontainersBase`) and spin up a real MySQL container, so Docker must be running for `./mvnw test` there to pass fully.

### Quick pipeline smoke test

```bash
curl -X POST http://localhost:8082/api/v1/ingestion \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":1,"energyConsumed":1200,"timestamp":"2025-01-01T12:00:00Z"}'
```

This bypasses the gateway/JWT (ingestion-service has no Spring Security) — check `usage-service` logs/TimescaleDB (`docker exec timescaledb psql -U postgres -d usage_timeseries -c "SELECT * FROM energy_usage ORDER BY time DESC LIMIT 5;"`), Kafka UI (`localhost:8070`), and Mailpit (`localhost:8025`) for downstream effects.

## Architecture notes that span multiple files

- **Package naming**: `com.jamlapps.<service>_service` (underscored service suffix, e.g. `com.jamlapps.ingestion_service`). Shared Kafka event classes live in a sibling `com.jamlapps.kafka.event` package inside each service that needs them (duplicated per-service, not a shared module) — see `EnergyUsageEvent`/`AlertingEvent` in `ingestion-service`, `usage-service`, and `alert-service`.
- **Mixed Spring Boot versions**: everything is Spring Boot 4.0.x except `insight-service`, which is pinned to Spring Boot 3.5.9 for Spring AI/Ollama compatibility. Don't assume dependency versions are uniform across services.
- **No config server**: each service owns its own `src/main/resources/application.properties`; there is no Spring Cloud Config Server. Cross-cutting config (Kafka bootstrap servers, DB URLs) must be edited per-service.
- **Event flow**: `ingestion-service` produces to Kafka topic `energy-usage-v2` → `usage-service` consumes (two CQRS listeners: one writes the TimescaleDB `energy_usage` hypertable, one writes a MySQL read-model with idempotency dedup), and (on threshold breach) produces to `energy-alerts-v2` → `alert-service` consumes and sends email via SMTP (Mailpit locally). Events are Avro, generated per-service from `.avsc` files via `avro-maven-plugin`, validated against a Confluent Schema Registry (`http://localhost:8086`) — not hand-written POJOs or plain JSON.
- **Gateway routing/security**: routes are defined as Java route classes under `api-gateway/src/main/java/com/jamlapps/api_gateway/route/` (one per backend service), each wrapped with a Resilience4j circuit breaker. The gateway is an OAuth2 Resource Server validating JWTs from Keycloak; unauthenticated path exceptions are listed under `security.excluded.urls` in the gateway's `application.properties` (actuator, swagger, api-docs). Note `/api/v1/ingestion/**` is *not* excluded through the gateway — for unauthenticated testing, hit `ingestion-service` directly on 8082 instead of going through the gateway on 9000.
- **Host-vs-container networking**: Prometheus (running in Docker) scrapes services via `host.docker.internal`, so run the Spring Boot services on the host (not containerized) for metrics to appear in Prometheus/Grafana. Correspondingly, host-run services must use Kafka's external listener at `localhost:9094`, not the in-network broker address.
- **Two MySQL instances**: the app database (`home_energy_tracker`, initialized by `docker/mysql/init.sql`) is separate from `keycloak-db`, a dedicated MySQL instance for Keycloak in `docker-compose.yml`.
- **TimescaleDB is the live time-series store** (migrated off InfluxDB, which has been decommissioned — container removed from `docker-compose.yml`, all InfluxDB code/deps/properties deleted from `usage-service`): `usage-service` connects to a `timescaledb` Postgres container (host port 5433 — 5432 is taken by a host-level PostgreSQL service on this machine, db `usage_timeseries`) via **two** `DataSource` beans in `config/DataSourceConfig.java` — `mysqlDataSource` (`@Primary`, unchanged, Boot's Flyway/JPA autoconfiguration keeps targeting MySQL) and `timescaleDataSource` (postgres superuser, used only by `config/TimescaleMigrationRunner.java` to run Flyway DDL against `db/timescale/`, history table `flyway_schema_history_usage_timescale`, kept out of the default `db/migration` location so MySQL's Flyway run doesn't recursively pick it up). Application reads/writes go through a **third** connection, `timescaleAppDataSource`/`timescaleAppJdbcTemplate`, authenticating as the restricted `usage_app` role (created by `db/timescale/V2__app_role.sql`) rather than the `postgres` superuser — superusers bypass row-level security, so using them for app traffic would make the `energy_usage` hypertable's `tenant_id` RLS policy a no-op. `command/TimescaleEnergyUsageWriter.java` writes raw telemetry (upserting on `(time, device_id)` to match InfluxDB's old last-write-wins-per-timestamp semantics); `alerting/UsageAlertingScheduler.java`'s 1-hour rolling threshold check is a plain SQL query against the same hypertable. Compression was deliberately **not** enabled on the hypertable — TimescaleDB doesn't allow compression (columnstore) and row-level security on the same table simultaneously, and RLS was prioritized.
