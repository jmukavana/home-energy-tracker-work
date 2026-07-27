CREATE TABLE energy_usage (
    time            TIMESTAMPTZ NOT NULL,
    device_id       BIGINT NOT NULL,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    energy_consumed DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (time, device_id)
);

SELECT create_hypertable('energy_usage', 'time');

SELECT add_retention_policy('energy_usage', INTERVAL '90 days');

-- Tenant isolation, built in now per Phase 0/4's decision to pull this forward.
-- COALESCEs to tenant 1 when the app hasn't set the session GUC (i.e. right now,
-- since TenantContext/JWT propagation doesn't land until Phase 4) — RLS is real
-- and enabled from day one, but safely permissive for the single-tenant present.
ALTER TABLE energy_usage ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON energy_usage
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_tenant_id', true), '')::bigint, 1));

-- NOT doing (real, verified TimescaleDB limitation, not a choice deferred for
-- convenience): compression ("columnstore") and row-level security cannot
-- coexist on the same hypertable — `ALTER TABLE ... SET (timescaledb.compress)`
-- fails outright with "columnstore cannot be used on table with row security"
-- once RLS is enabled. Since nothing writes to this table until Phase 2b,
-- compression has zero effect today either way; revisit this trade-off (drop
-- RLS in favor of compression, or keep RLS and accept uncompressed storage)
-- once real write volume from 2b makes the storage-cost side of the trade-off
-- concrete rather than theoretical.
