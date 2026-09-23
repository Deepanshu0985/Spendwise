# Process — What's Done, What's Left

Live status tracker, updated as work happens. For the full plan with durations and dependencies, see `docs/09-project/phase-plan.md`. For why a choice was made, see `DECISIONS.md` (implementation-level) or `docs/02-architecture/architecture-decisions.md` (formal ADRs).

## Phase 0 — Foundation and First Deploy

**Done:**
- [x] Maven project scaffold (`backend/pom.xml`) — Spring Boot 3.3.5, Java 21
- [x] Spring configuration via `application.properties` + `dev`/`test`/`prod` profiles, no YAML/XML config
- [x] Module package skeleton under `com.finance.*` matching `docs/02-architecture/lld.md`
- [x] Common error-handling foundation: `ApiResponse`, `ApiError`, `GlobalExceptionHandler`, `ApiException` hierarchy, matching `docs/03-api/error-contract.md`
- [x] Health endpoint (`GET /api/v1/health`)
- [x] Flyway wired to boot against an empty migration set
- [x] Local dev database: file-based H2, no Docker (ADR-018)
- [x] JDK toolchain fixed: pinned to `openjdk@21`, IntelliJ Project SDK corrected (see `DECISIONS.md`)
- [x] Verified: `mvn compile`, `mvn test`, `mvn spring-boot:run` + health-check curl all pass

- [x] React/TypeScript frontend skeleton (Vite, strict mode) with routing for all `screen-specification.md` screens and a working API client layer — verified: `npm run build` (tsc strict + vite build) passes, dev server rendered in-browser, live health check through the Vite proxy confirmed "Backend connectivity: up"
- [x] `docker-compose.yml` + `backend/Dockerfile` + `frontend/Dockerfile` + root `Caddyfile` (staging/production shape from `deployment.md`) — verified with a local Colima Docker runtime: both images build clean, all four containers (caddy, backend, frontend, db) start and reach healthy, backend connects to real PostgreSQL and runs Flyway against it, backend and frontend both respond correctly over the compose network. Colima was removed after validation; local dev still doesn't use Docker (ADR-018)
- [x] `scripts/nightly-backup.sh` and `scripts/restore-drill.sh` authored per `deployment.md` (row-count check now; TODO to wire in the reconciliation-invariant test run once Phase 4 exists)
- [x] `.github/workflows/ci.yml` (backend `mvn verify`, frontend lint + build) — authored and YAML-valid; cannot be verified running until pushed to an actual GitHub repo (see below)
- [x] Pushed to [github.com/Deepanshu0985/Spendwise](https://github.com/Deepanshu0985/Spendwise), `main` branch. CI (`ci.yml`) confirmed green on GitHub Actions for both the initial push and a follow-up fix (bumped `actions/setup-java` from v4, flagged deprecated by CI's own first run, to v5)

**Decision (2026-09-23): deploy step deliberately deferred.** Per the user, the plan is to build the product locally first and only provision DigitalOcean + the domain at the end, rather than deploying continuously from Phase 0 onward. `docker-compose.yml`/`Caddyfile`/`Dockerfile`s/backup scripts are written and locally validated (see `DECISIONS.md`) but nothing is deployed yet, and `ci.yml` intentionally has no deploy step.

**Left — deferred until the end of the build, needs your action then, not more code now:**
- [ ] Provision the DigitalOcean droplet + Spaces bucket (ADR-017) — requires your DigitalOcean account/payment method
- [ ] Register/point a real domain at the droplet — `APP_DOMAIN` is currently a placeholder (`example.com`); Caddy's automatic HTTPS is confirmed working correctly and will obtain a real cert the moment this points at a real, publicly-reachable domain
- [ ] Add deploy secrets to GitHub Actions (SSH key to the droplet, registry credentials) and add the actual deploy step to `ci.yml`
- [ ] First live deploy to production, completing the Phase 0 exit gate ("a push to main reaches production automatically")
- [ ] Nightly backup cron/systemd timer actually scheduled on the droplet (script is ready, not yet installed anywhere)

## Phase 1 — Authentication and Isolation
Not started.

## Phase 2 — Accounts and Categories
Not started.

## Phase 3 — Transactions
Not started.

## Phase 4 — Analytics
Not started.

## Phase 5 — Internal Dogfooding
Not started.

## Phase 6 — Statement Import
Not started.

## Phase 7 — Normalization and Duplicates
Not started.

## Phase 8 — Dogfood Statements
Not started.

## Phase 9 — Recurring Detection
Not started.

## Phase 10 — Budgets and Goals
Not started.

## Phase 11 — AI Categorization
Not started. Blocked on D-03 (LLM provider and model) — see `docs/09-project/open-decisions.md`.

## Phase 12 — AI Assistant
Not started.

## Phase 13 — AI Insights
Not started.

## Phase 14 — Beta Readiness
Not started.
