# Backup and Restore (Neon)

The database is Neon PostgreSQL. This replaces the VPS-era `nightly-backup.sh` / `restore-drill.sh` for the deployment that actually exists (Render + Vercel + Neon); those two scripts remain only for the self-hosted layout in `deployment.md`.

## What protects the data

1. **Neon's own history.** Neon keeps a change history that lets you restore a branch to an earlier moment, or create a new branch "from a past point". The window depends on the plan (the free plan's is short: hours, not days), so check it in the Neon console under the project's settings and write the number here: **window: ______** (fill in).
2. **A logical dump you keep yourself.** `pg_dump` output is the only copy that does not depend on one vendor's retention rules. Take one before risky changes (a migration that drops or rewrites data, a bulk delete) and at least monthly:

   ```bash
   docker run --rm postgres:16-alpine pg_dump 'POSTGRES_URL_OF_NEON_WITH_SSLMODE=require' \
     --no-owner --no-privileges --format=custom > spendwise-$(date +%Y%m%d).dump
   ```

   A dump contains every user's financial data. Keep it encrypted and off shared drives, and delete old ones; it is subject to the same deletion promise as the database (a person who deletes their account is still in an old dump, so dumps should not outlive the retention stated in the privacy policy).
3. **The migrations in git.** Flyway builds the schema from nothing, so the structure is always recoverable; only the data needs a backup.

## The restore drill

`scripts/restore-drill-neon.sh` proves a dump comes back whole. It dumps the source, restores into a throwaway PostgreSQL in Docker, then compares every table's row count and checks that the migration history, the row-level-security policies, `FORCE ROW LEVEL SECURITY` and the foreign keys all survived. The security checks are the point: a restore that quietly dropped the policies would let every user read everyone's rows.

```bash
SOURCE_URL='postgresql://USER:PASSWORD@HOST/DB?sslmode=require' ./scripts/restore-drill-neon.sh
```

It only reads the source. Run it:

- **Monthly**, against the live branch (proves a fresh dump restores).
- **Before the beta opens and after any schema-changing release**, against a Neon branch created "from a past point in time" (Neon console, Branches, Create branch, past data), which rehearses a real point-in-time recovery.

Write each run in the log below. A drill that has not been run recently is a failed drill.

### Drill log

| Date | Source | Result | By |
|---|---|---|---|
| 2026-10-11 | Local PostgreSQL with test data from the integration suite (24 migrations, 21 tables, 19 RLS policies, 16 tables with forced row security, 38 foreign keys) | Passed; counts and safety properties identical | Claude, to prove the script works |
| _next_ | **Neon production or a point-in-time branch** | _to be run and signed off by the owner before beta_ | |

The first row shows the script works; it is **not** the sign-off. The roadmap's "restore drill signed off" means a run against Neon, recorded above.

## Restoring for real

1. Stop writes if you can: set `AI_ENABLED=false` and suspend the Render service (or scale it to zero) so nothing writes to a half-restored database.
2. Choose the source: a Neon point-in-time branch, or a dump. For a Neon branch, make the branch from the last good moment, check it with the drill script, then point `DATABASE_URL` (and the Flyway variables) at it in Render. For a dump, create an empty Neon branch, run `pg_restore --no-owner --no-privileges -d <new-branch-url> <dump>`, check, then switch `DATABASE_URL`.
3. Make sure the two database roles exist and the application role has no `BYPASSRLS` (see `DECISIONS.md`, "Neon Postgres replaces H2": the grants are listed there). A restore does not carry roles or grants (`--no-owner --no-privileges`), so re-run them.
4. Resume the service and test sign-in and one page of data.
5. Users who deleted their account since the backup are present in it again. Re-run their deletions (the support list is the owner's record of requests) before reopening, because the privacy policy promises deletion is permanent.
