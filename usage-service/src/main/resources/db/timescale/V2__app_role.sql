-- Phase 2b: a restricted, non-superuser role for the application's own reads/writes.
-- The bootstrap `postgres` role (used by TimescaleMigrationRunner to run this very
-- migration) is a superuser and superusers bypass row-level security entirely —
-- pointing the app's runtime connection at `postgres` would make the RLS policy
-- from V1 a no-op. This role is scoped to exactly the one table it needs.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'usage_app') THEN
        CREATE ROLE usage_app LOGIN PASSWORD 'usage_app_password';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE usage_timeseries TO usage_app;
GRANT USAGE ON SCHEMA public TO usage_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON energy_usage TO usage_app;
