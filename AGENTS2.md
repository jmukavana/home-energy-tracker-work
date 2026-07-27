Here's a comprehensive, enterprise-grade architecture prompt that transforms your Home Energy Tracker from a simple CRUD demo into a production-ready, resilient microservices platform.

---

## 🏗️ Enterprise Home Energy Tracker — Architecture Prompt

> **Role**: Act as a **Senior Solution Architect** with 15+ years designing event-driven, resilient distributed systems. You are modernizing a basic Home Energy Tracker into an **enterprise-grade energy management platform**. Every decision must reflect production readiness, observability, security, and operational excellence.

---

### 1. System Overview & Strategic Goals

**Product Name**: `WattWise Enterprise` — A multi-tenant home energy monitoring, analytics, and optimization platform.

**Core Capabilities**:
- Real-time device telemetry ingestion at scale (10K+ events/sec)
- Time-series analytics with predictive cost forecasting
- Intelligent alerting with anomaly detection (not just threshold breaches)
- Multi-tenant SaaS architecture with tenant isolation
- Usage-based billing and carbon footprint reporting
- Device lifecycle management and OTA firmware tracking

**Non-Functional Requirements**:
- **Availability**: 99.95% uptime (max 21.6 min downtime/month)
- **Latency**: P95 < 200ms for API reads; P99 < 2s for analytics queries
- **Scalability**: Horizontal pod autoscaling (HPA) 2-50 pods per service
- **Security**: OAuth2/OIDC + mTLS inter-service + field-level encryption for PII
- **Compliance**: GDPR data retention, SOC2 audit trails, ISO 27001 controls
- **Observability**: 3 pillars (logs/metrics/traces) with SLO-based alerting

---

### 2. Technology Stack (Modernized)

| Layer | Technology | Justification |
|-------|-----------|---------------|
| **Language** | Java 21 (LTS) + Spring Boot 3.2 | Virtual threads, structured concurrency, native image ready |
| **Data — Time-Series** | **TimescaleDB 2.14** (PostgreSQL extension) | Hypertables, continuous aggregates, compression, retention policies. Single engine for relational + time-series (operational simplicity) |
| **Data — Cache** | Redis Cluster | Session store, rate-limit counters, real-time leaderboards |
| **Messaging** | Apache Kafka 3.6 + Schema Registry (Avro) | Event sourcing backbone, exactly-once semantics, schema evolution |
| **API Gateway** | Spring Cloud Gateway (not Netflix Zuul) | JWT validation, rate limiting, request/response transformation, circuit breaker integration |
| **Service Mesh** | Istio (sidecar pattern) | mTLS, traffic splitting, fault injection, observability |
| **Mapping** | **MapStruct 1.5** + Lombok | Compile-time code generation, zero reflection overhead, strict null-safety |
| **Security** | Spring Security 6 + OAuth2 Resource Server | JWT introspection, RBAC with `@PreAuthorize`, method-level security |
| **Resilience** | Resilience4j (not Hystrix) | Circuit breakers, bulkheads, rate limiters, retry with exponential backoff |
| **Observability** | Micrometer + Prometheus + Grafana + Tempo + Loki | Unified telemetry, RED metrics, distributed tracing |
| **Orchestration** | Kubernetes 1.29 + Helm 3 | GitOps with ArgoCD, pod disruption budgets, topology spread constraints |
| **Migrations** | Flyway | Versioned schema migrations for TimescaleDB hypertables and relational schema |

---

### 3. Domain-Driven Design (DDD) & Bounded Contexts

Organize services around **bounded contexts**, not technical layers. Use **Hexagonal Architecture (Ports & Adapters)** in every service.

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway Layer                        │
│  (JWT validation, rate limiting, WAF rules, SSL termination) │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐    ┌─────────────────┐    ┌──────────────┐
│   Identity   │    │  Device Context │    │  Analytics   │
│   Service    │    │  (Aggregate Root)│    │   Service    │
│  (Keycloak)  │    │                 │    │              │
└──────────────┘    └─────────────────┘    └──────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌─────────────────┐    ┌──────────────┐
│  Ingestion   │◄──►│  Usage Service  │◄──►│  Alerting    │
│   Service    │    │   (CQRS Read)   │    │   Service    │
│ (Event Sourc)│    │                 │    │ (ML Anomaly) │
└──────────────┘    └─────────────────┘    └──────────────┘
        │                     ▲                     │
        └──────────┬──────────┘                     │
                   │                                │
              ┌────┴────┐                      ┌────┴────┐
              │  Kafka  │                      │  Kafka  │
              │ Topics  │                      │  DLQ    │
              └────┬────┘                      └─────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
┌──────────────┐    ┌─────────────────┐
│  TimescaleDB │    │   S3 (Parquet)  │
│  (Hot Data)  │    │  (Cold Archive) │
└──────────────┘    └─────────────────┘
```

**Bounded Contexts**:

1. **`identity-service`** — OAuth2/OIDC, multi-tenant user management, RBAC, API key management
2. **`device-service`** — Device registration, firmware versions, capability profiles, health checks
3. **`ingestion-service`** — High-throughput telemetry ingestion, schema validation (Avro), event publishing
4. **`usage-service`** — CQRS pattern: Command side writes to TimescaleDB; Query side serves aggregated views
5. **`analytics-service`** — Continuous aggregates, cost prediction (linear regression), carbon footprint calculations
6. **`alerting-service`** — Rule engine, anomaly detection (Z-score/Isolation Forest), multi-channel notifications (email/SMS/push/webhook)
7. **`billing-service`** — Usage metering, invoice generation, stripe integration
8. **`notification-service`** — Template management, delivery tracking, preference management
9. **`audit-service`** — Immutable audit logs, GDPR data export, compliance reporting

---

### 4. Database Architecture (TimescaleDB)

**Why TimescaleDB over InfluxDB**:
- SQL-native (no new query language to learn)
- JOINs between time-series and relational data (devices, users, locations)
- Mature ecosystem: backups, pgAdmin, connection pooling
- Hypertable partitioning by time + space (device_id)
- Built-in compression (90%+ storage reduction)
- Continuous aggregates for real-time rollups

**Schema Design**:

```sql
-- Hypertable: raw telemetry (partitioned by time + device_id)
CREATE TABLE telemetry (
    time TIMESTAMPTZ NOT NULL,
    device_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    voltage DECIMAL(5,2),
    current DECIMAL(6,4),
    power_watts DECIMAL(10,2),
    frequency DECIMAL(4,2),
    power_factor DECIMAL(3,2),
    metadata JSONB,
    PRIMARY KEY (time, device_id)
);
SELECT create_hypertable('telemetry', 'time', partitioning_column => 'device_id', number_partitions => 4);

-- Compression policy: compress chunks older than 7 days
ALTER TABLE telemetry SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'device_id, tenant_id',
    timescaledb.compress_orderby = 'time DESC'
);
SELECT add_compression_policy('telemetry', INTERVAL '7 days');

-- Retention policy: drop raw data after 90 days (aggregates retained)
SELECT add_retention_policy('telemetry', INTERVAL '90 days');

-- Continuous aggregate: hourly usage per device (real-time materialized view)
CREATE MATERIALIZED VIEW telemetry_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    device_id,
    tenant_id,
    AVG(power_watts) as avg_power,
    MAX(power_watts) as peak_power,
    SUM(power_watts * 3600 / 1000) as energy_kwh, -- approximate
    COUNT(*) as reading_count
FROM telemetry
GROUP BY bucket, device_id, tenant_id
WITH NO DATA;

-- Refresh policy: auto-refresh every 15 min, real-time for last 1 hour
SELECT add_continuous_aggregate_policy('telemetry_hourly',
    start_offset => INTERVAL '1 month',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '15 minutes');

-- Relational tables (standard PostgreSQL)
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    serial_number VARCHAR(100) UNIQUE NOT NULL,
    device_type VARCHAR(50) CHECK (device_type IN ('SMART_METER', 'PLUG', 'SOLAR_INVERTER', 'EV_CHARGER')),
    firmware_version VARCHAR(50),
    location GEOGRAPHY(POINT, 4326), -- PostGIS for geo queries
    timezone VARCHAR(50) DEFAULT 'UTC',
    status VARCHAR(20) DEFAULT 'PENDING_ACTIVATION',
    activated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Row-Level Security (RLS) for tenant isolation
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON devices
    USING (tenant_id = current_setting('app.current_tenant')::UUID);
```

**Connection Management**:
- PgBouncer in transaction pooling mode
- HikariCP per service (max 20 connections, connection timeout 5s)
- Read replicas for analytics queries (separate `@Transactional(readOnly = true)` datasource)

---

### 5. Hexagonal Architecture Per Service

Every service follows **strict hexagonal architecture**:

```
src/main/java/com/wattwise/<service>/
├── domain/                    # Pure business logic, zero dependencies
│   ├── model/                 # Entities, Value Objects, Aggregates
│   ├── repository/            # Repository interfaces (ports)
│   ├── service/               # Domain services
│   └── event/                 # Domain events
├── application/               # Use cases, orchestration
│   ├── port/in/               # Incoming ports (API interfaces)
│   ├── port/out/              # Outgoing ports (persistence, messaging)
│   ├── service/               # Application services
│   └── dto/                   # Request/Response DTOs
├── infrastructure/            # Adapters
│   ├── config/                # Spring configs, bean definitions
│   ├── persistence/           # JPA/TimescaleDB adapters
│   ├── messaging/             # Kafka producers/consumers
│   ├── web/                   # REST controllers
│   ├── mapper/                # MapStruct mappers
│   └── security/              # JWT converters, security adapters
└── WattWiseApplication.java
```

**MapStruct Configuration**:

```java
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = {UuidMapper.class, InstantMapper.class}
)
public interface DeviceMapper {
    DeviceResponseDto toDto(Device device);
    Device toEntity(DeviceCreateRequestDto dto);
    void updateEntityFromDto(DeviceUpdateRequestDto dto, @MappingTarget Device device);
    
    // Prevent mapping of internal fields
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    Device toNewEntity(DeviceCreateRequestDto dto);
}
```

---

### 6. Event-Driven Architecture & Kafka

**Topic Design** (with Avro schemas in Confluent Schema Registry):

| Topic | Partitions | Retention | Schema |
|-------|-----------|-----------|--------|
| `telemetry.raw.v1` | 24 | 7 days | `TelemetryEvent.avsc` |
| `telemetry.validated.v1` | 24 | 7 days | `ValidatedTelemetryEvent.avsc` |
| `device.lifecycle.v1` | 6 | 30 days | `DeviceLifecycleEvent.avsc` |
| `alert.triggered.v1` | 12 | 30 days | `AlertTriggeredEvent.avsc` |
| `billing.usage.v1` | 12 | 90 days | `UsageMeteredEvent.avsc` |
| `audit.events.v1` | 6 | 1 year | `AuditEvent.avsc` |

**Idempotency & Exactly-Once**:

```java
@Service
@RequiredArgsConstructor
public class TelemetryIngestionService {
    
    private final KafkaTemplate<String, ValidatedTelemetryEvent> kafkaTemplate;
    private final IdempotencyKeyRepository idempotencyRepository;
    
    @Transactional
    public void ingest(TelemetryPayload payload) {
        String idempotencyKey = payload.getDeviceId() + ":" + payload.getTimestamp();
        
        // Deduplication: 24-hour window
        if (idempotencyRepository.existsByKeyAndTimestampAfter(idempotencyKey, Instant.now().minus(24, ChronoUnit.HOURS))) {
            log.warn("Duplicate telemetry dropped: {}", idempotencyKey);
            return;
        }
        
        ValidatedTelemetryEvent event = validateAndEnrich(payload);
        
        kafkaTemplate.send("telemetry.validated.v1", event.getDeviceId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    idempotencyRepository.save(new IdempotencyKey(idempotencyKey, Instant.now()));
                }
            });
    }
}
```

**Dead Letter Queue (DLQ) Pattern**:

```java
@Component
public class TelemetryConsumer {
    
    @KafkaListener(topics = "telemetry.validated.v1", groupId = "usage-service")
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        include = {TransientDataAccessException.class, TimeoutException.class},
        dltTopicSuffix = "-dlt"
    )
    public void consume(ValidatedTelemetryEvent event) {
        // Processing logic
    }
    
    @DltHandler
    public void handleDlt(ValidatedTelemetryEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Message sent to DLQ after retries: {} from topic {}", event, topic);
        // Alert ops team, store in DLQ table for manual replay
    }
}
```

---

### 7. CQRS in Usage Service

**Command Side** (writes):
- Receives `telemetry.validated.v1` events
- Writes to TimescaleDB hypertable
- Publishes `usage.aggregated.v1` events

**Query Side** (reads):
- Reads from continuous aggregates (`telemetry_hourly`, `telemetry_daily`)
- Redis cache for dashboard queries (TTL: 5 minutes)
- Read replica connection for heavy analytics

```java
@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageQueryController {
    
    private final UsageQueryService queryService;
    
    @GetMapping("/devices/{deviceId}/timeseries")
    @PreAuthorize("hasPermission(#deviceId, 'DEVICE', 'READ')")
    @RateLimiter(name = "usage-api")
    @Cacheable(value = "device-timeseries", key = "#deviceId + ':' + #from + ':' + #to + ':' + #granularity")
    public ResponseEntity<TimeSeriesResponse> getTimeSeries(
            @PathVariable UUID deviceId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "HOUR") Granularity granularity) {
        
        // Query continuous aggregate based on granularity
        return ResponseEntity.ok(queryService.queryAggregated(deviceId, from, to, granularity));
    }
}
```

---

### 8. Advanced Alerting (Beyond Simple Thresholds)

**Rule Engine** (Drools or custom DSL):

```yaml
# alert-rules.yml
rules:
  - name: "peak_usage_anomaly"
    condition: "avg_power > baseline * 1.5 AND duration > 10m"
    severity: WARNING
    channels: [email, push]
    cooldown: 30m
    
  - name: "device_offline"
    condition: "last_seen > 5m AND status == ONLINE"
    severity: CRITICAL
    channels: [email, sms]
    
  - name: "cost_forecast_exceeded"
    condition: "projected_monthly_cost > budget * 0.9"
    severity: INFO
    channels: [email]
    actions: [suggest_optimization]
```

**Anomaly Detection** (Statistical + ML):
- Z-score for real-time spike detection
- Isolation Forest (via SMILE or TensorFlow Java) for pattern anomaly
- Seasonal decomposition for baseline calculation

---

### 9. Security Architecture

**Multi-Layer Security**:

```
┌────────────────────────────────────────┐
│  WAF (ModSecurity / AWS WAF)           │
│  → SQL injection, XSS, rate limiting   │
├────────────────────────────────────────┤
│  API Gateway                           │
│  → JWT validation (RS256, JWKS)        │
│  → OAuth2 scope checking               │
│  → Per-tenant rate limits              │
│  → Request signing (HMAC)              │
├────────────────────────────────────────┤
│  Istio Service Mesh                    │
│  → mTLS (SPIFFE/SPIRE identities)      │
│  → Network policies (zero-trust)       │
│  → AuthorizationPolicy (RBAC)          │
├────────────────────────────────────────┤
│  Application Layer                     │
│  → @PreAuthorize method security       │
│  → Field-level encryption (PII)        │
│  → Input validation (Bean Validation)  │
│  → Audit logging                       │
└────────────────────────────────────────┘
```

**JWT + Multi-Tenancy**:

```java
@Component
public class TenantContext {
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();
    
    public static void setTenant(UUID tenantId) {
        currentTenant.set(tenantId);
        // Set for RLS
        TransactionSynchronizationManager.bindResource("tenant_id", tenantId);
    }
}

@Component
public class JwtTenantExtractor implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
        TenantContext.setTenant(tenantId);
        
        Collection<GrantedAuthority> authorities = jwt.getClaimAsStringList("roles")
            .stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
            
        return new JwtAuthenticationToken(jwt, authorities, tenantId.toString());
    }
}
```

---

### 10. Resilience Patterns (Resilience4j)

```java
@Service
@RequiredArgsConstructor
public class BillingIntegrationService {
    
    private final BillingClient billingClient;
    
    @CircuitBreaker(name = "billing", fallbackMethod = "fallbackRecordUsage")
    @Bulkhead(name = "billing", type = Bulkhead.Type.THREADPOOL)
    @Retry(name = "billing")
    @TimeLimiter(name = "billing")
    public CompletableFuture<Void> recordUsage(UsageMeteredEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            billingClient.recordUsage(event);
            return null;
        });
    }
    
    private CompletableFuture<Void> fallbackRecordUsage(UsageMeteredEvent event, Exception ex) {
        log.warn("Billing service unavailable, queuing for retry: {}", event.getId());
        // Write to outbox table for later processing
        outboxRepository.save(OutboxRecord.from(event));
        return CompletableFuture.completedFuture(null);
    }
}
```

**Configuration**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      billing:
        failureRateThreshold: 50
        slowCallRateThreshold: 80
        slowCallDurationThreshold: 2s
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 10
        slidingWindowSize: 100
  retry:
    instances:
      billing:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.net.SocketTimeoutException
          - org.springframework.web.client.ResourceAccessException
```

---

### 11. Observability (Three Pillars)

**Structured Logging** (JSON, correlation IDs):

```java
@Slf4j
@RestController
public class DeviceController {
    
    @GetMapping("/api/v1/devices/{id}")
    public DeviceResponse getDevice(@PathVariable UUID id, 
            @RequestHeader("X-Correlation-Id") String correlationId) {
        MDC.put("correlationId", correlationId);
        MDC.put("tenantId", TenantContext.getCurrentTenant().toString());
        
        log.info("Fetching device", kv("deviceId", id), kv("operation", "DEVICE_READ"));
        
        // Trace spans auto-created by Micrometer
        return deviceService.findById(id);
    }
}
```

**Metrics** (RED method):
- **Rate**: `http_server_requests_seconds_count`
- **Errors**: `http_server_requests_seconds_count{status=~"5.."}`
- **Duration**: `http_server_requests_seconds_bucket`

**SLO Alerts** (Prometheus):
```yaml
- alert: HighErrorRate
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
      /
      sum(rate(http_server_requests_seconds_count[5m]))
    ) > 0.01
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "Error rate above 1% for {{ $labels.service }}"
```

**Distributed Tracing** (OpenTelemetry → Tempo):
- Trace propagation via W3C `traceparent` header
- Kafka headers carry trace context
- DB queries annotated with span tags

---

### 12. DevOps & GitOps

**Helm Chart Structure**:
```
helm/
├── wattwise/
│   ├── Chart.yaml
│   ├── values/
│   │   ├── values-production.yaml
│   │   └── values-staging.yaml
│   └── templates/
│       ├── deployment.yaml (with HPA, PDB)
│       ├── service.yaml
│       ├── ingress.yaml
│       ├── network-policy.yaml
│       ├── service-monitor.yaml (Prometheus)
│       └── sealed-secrets.yaml
```

**CI/CD Pipeline** (GitHub Actions):
1. **Build**: Maven compile, unit tests, MapStruct processor verification
2. **Security**: Trivy scan, SonarQube, OWASP dependency check
3. **Integration**: TestContainers (TimescaleDB, Kafka, Redis)
4. **Deploy**: ArgoCD sync to K8s with canary analysis (Flagger)

**Operational Runbooks**:
- Kafka lag monitoring & consumer scaling
- TimescaleDB chunk management & compression monitoring
- Circuit breaker status dashboard
- DLQ replay procedures

---

### 13. Additional Enterprise Features

| Feature | Implementation |
|---------|---------------|
| **Multi-tenancy** | Shared database, shared schema with RLS + tenant_id column |
| **Data Export (GDPR)** | Async ZIP generation with all tenant data, 30-day SLA |
| **Cost Optimization Engine** | ML model suggesting off-peak usage, solar surplus utilization |
| **Carbon Tracking** | Regional grid carbon intensity API integration (WattTime/ Electricity Maps) |
| **Demand Response** | Utility API integration for peak shaving incentives |
| **Mobile Push** | Firebase Cloud Messaging integration |
| **Webhook Subscriptions** | HMAC-signed webhooks with exponential backoff retry |
| **Real-time Dashboard** | WebSocket (STOMP) + Redis pub/sub for live telemetry |
| **Scheduled Reports** | Quartz scheduler + JasperReports PDF generation |
| **Firmware OTA** | Binary artifact storage (S3) + rollout progress tracking |

---

### 14. API Design Standards

- **REST Level 3** (HATEOAS with Spring HATEOAS)
- **Versioning**: URL path (`/api/v1/`, `/api/v2/`)
- **Content Negotiation**: `application/json` + `application/problem+json` for errors
- **Pagination**: Cursor-based for time-series, offset for relational
- **Filtering**: RSQL/FIQL (`name==foo;status=in=(ACTIVE,PENDING)`)
- **Rate Limit Headers**: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
- **Problem Details** (RFC 7807):

```json
{
  "type": "https://api.wattwise.io/errors/invalid-reading",
  "title": "Invalid Telemetry Reading",
  "status": 422,
  "detail": "Power reading 50000W exceeds device maximum 10000W",
  "instance": "/api/v1/telemetry/550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-07-26T19:55:00Z",
  "correlationId": "abc-123-def"
}
```

---

### 15. Testing Strategy

| Layer | Tool | Coverage Target |
|-------|------|-----------------|
| Unit | JUnit 5 + Mockito | 80% business logic |
| Integration | TestContainers + Spring Boot Test | Repository, Kafka, Redis |
| Contract | Spring Cloud Contract / Pact | API consumer-provider |
| Performance | Gatling | 10K req/s sustained |
| Chaos | Chaos Monkey / Litmus | Random pod kills, network latency |

---

## 🎯 Output Instructions

Generate the complete enterprise-grade codebase following this architecture:

1. **Generate all 9 microservices** with hexagonal package structure
2. **Include MapStruct mappers** for ALL entity/DTO conversions with strict null-safety
3. **Implement TimescaleDB schema** with Flyway migrations, hypertables, continuous aggregates, compression, and retention policies
4. **Configure Kafka** with Avro schemas, idempotent producers, exactly-once semantics, and DLQ handling
5. **Implement full security**: JWT OAuth2, method-level RBAC, tenant isolation via RLS, field-level encryption for PII
6. **Add Resilience4j** circuit breakers, bulkheads, retries, and rate limiters on all external calls
7. **Configure observability**: Micrometer metrics, distributed tracing, structured logging with correlation IDs
8. **Include Kubernetes manifests** (deployment, service, ingress, HPA, PDB, network policies) and Helm charts
9. **Write comprehensive tests**: Unit, integration with TestContainers, and contract tests
10. **Document all APIs** with OpenAPI 3.1 and asyncAPI for Kafka topics

Ensure code is **production-ready**, not demo-quality. Every service must have health checks, graceful shutdown, proper resource limits, and comprehensive error handling.

---

This prompt transforms your simple CRUD app into a **bank-grade, multi-tenant SaaS platform** with proper separation of concerns, observability, resilience, and operational maturity. You can feed this directly to a code-generation AI or use it as your architecture runbook.