-- Creates the restricted runtime role (NOBYPASSRLS) the application's own
-- connection uses, separate from the privileged role migrations run under.
-- Without this, RLS is silently ineffective for real traffic even with
-- FORCE ROW LEVEL SECURITY set on every table - see ADR-010 and DECISIONS.md
-- ("Found: the runtime app connection had BYPASSRLS all along").
--
-- Run once per database, before the first Flyway migration, as a role with
-- CREATEROLE (e.g. the database owner):
--   psql "$SUPERUSER_URL" -v password="$APP_RUNTIME_PASSWORD" -f scripts/init-db-roles.sql
--
-- Idempotent: safe to re-run against a database that already has the role
-- (e.g. Neon's dev/prod branches, provisioned manually before this script
-- existed - see DECISIONS.md).

-- Not a DO $$ ... $$ block: psql's :'password' substitution does not apply
-- inside dollar-quoting, so it would be passed through literally instead of
-- substituted (caught by testing this script against a scratch Postgres
-- before trusting it in CI - see DECISIONS.md). \gexec runs whatever the
-- query returns as further SQL, so this is a no-op, not an error, when the
-- role already exists (the WHERE clause returns zero rows).
SELECT format('CREATE ROLE app_runtime WITH LOGIN PASSWORD %L NOBYPASSRLS NOSUPERUSER NOCREATEDB NOCREATEROLE', :'password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_runtime')
\gexec

GRANT USAGE ON SCHEMA public TO app_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_runtime;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_runtime;
