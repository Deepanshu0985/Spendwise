# Environments

Two environments until beta. A shared development environment and a permanent staging environment are both deferred, because each one is another deployment to pay for and maintain, and operational burden is the main risk to a solo build.

## Local

No Docker (ADR-018). The backend runs directly via Maven against a Neon Postgres branch (`dev` Spring profile, see `DECISIONS.md`); Flyway still owns the schema, so the migration history matches what production applies. A stub `AIModelClient` returning fixtures, so development does not consume provider tokens. Filesystem storage in place of object storage. See `backend/README.md` for run instructions.

The `test` profile still uses in-memory H2 for fast, isolated unit tests. Row-Level Security (ADR-010) cross-tenant isolation tests and the pooled-connection leakage test must run against real PostgreSQL — either the local Neon branch or CI's PostgreSQL service — never against H2.

## Production

The single VPS described in `deployment.md`.

## Staging

Added at the beta gate, not before. If budget is constrained at that point, a second Compose project on different ports on the same host is an acceptable stopgap.

## Configuration

```text
DATABASE_URL
DATABASE_USER
DATABASE_PASSWORD

STORAGE_ENDPOINT
STORAGE_BUCKET
STORAGE_ACCESS_KEY
STORAGE_SECRET_KEY

AI_PROVIDER
AI_API_KEY
AI_MODEL
AI_ENABLED
AI_USER_DAILY_TOKEN_CAP
AI_GLOBAL_MONTHLY_COST_CAP

SESSION_TTL
SESSION_IDLE_TIMEOUT

MAIL_PROVIDER
MAIL_API_KEY
MAIL_FROM

APP_BASE_URL
```

`AI_ENABLED` is a kill switch that can be flipped without a deploy. It is the control that limits damage when provider usage runs away unattended.

Never commit real credentials.
