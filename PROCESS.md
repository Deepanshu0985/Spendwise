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

**Done:**
- [x] Local dev database switched from H2 to a dedicated Neon Postgres dev branch, separate from the Neon branch designated for prod (see `DECISIONS.md`) — needed since RLS, this phase's core feature, has no H2 equivalent
- [x] Dev branch cleaned of two rounds of leftover unrelated demo objects from prior experiments on the same Neon project (including a colliding `users` table); prod branch left baselined around its own demo tables since it isn't in active use yet
- [x] `V1__create_users_table.sql`, `V2__create_sessions_table.sql`, `V3__create_password_reset_tokens_table.sql` — Postgres-native (gen_random_uuid(), TIMESTAMPTZ); `test` profile now skips Flyway entirely and lets Hibernate generate H2 schema from entities instead, since these migrations don't parse on H2 at all (see `DECISIONS.md`)
- [x] **ADR-019**: RLS deliberately excluded from these 3 tables (auth-bootstrap chicken-and-egg problem); isolation enforced by explicit `user_id` filtering instead, verified with two real users (Bob never sees Alice's sessions or profile)
- [x] `TenantContext`/`MutableTenantContext` + `ThreadLocalTenantContext`, `TenantContextAspect` (`@Order(1)`, inside the `@Order(0)` transactional advisor) issuing `SELECT set_config('app.current_user_id', ?, true)` at the start of every `@Transactional` method — confirmed firing correctly in real Postgres logs
- [x] `SessionStore` + impl, `SecureTokenGenerator`/`TokenHasher` (SHA-256, not bcrypt — tokens are already high-entropy, need indexed lookup), `PasswordEncoder` (BCrypt) bean — no full Spring Security, since it would conflict with ADR-009's custom cookie-session design
- [x] `AuthService`/`AuthServiceImpl`: register, login, logout, list/revoke-all sessions, password-reset request+confirm (with the reset flow **never logging the raw token**, per security.md — `EmailSender` is stubbed via `LoggingEmailSender` until Resend gets a real API key, D-04)
- [x] `AuthController`, `UserController` (`GET /users/me`), `SessionAuthenticationFilter`, `CsrfTokenFilter` (double-submit cookie), `SessionCleanupJob`
- [x] Verified end-to-end via curl against the real Neon dev branch: register, duplicate-email rejection (409), wrong-password rejection (401), login sets a correct `HttpOnly; Secure; SameSite=Lax` cookie, `/users/me`, session listing, logout, revoke-all, full password-reset request→confirm→login-with-new-password cycle, weak-password rejection (400), CSRF rejection (403) and acceptance, two-user session/profile isolation
- [x] Found and fixed two real bugs along the way (see `DECISIONS.md`): blank `.env` values silently defeating Spring's `${VAR:default}` fallback, and filters not being reachable by `@RestControllerAdvice`

**Left:**
- [ ] Automated JUnit/integration test suite codifying the above (currently verified manually via curl, not yet in the repo as tests)
- [ ] CI: add a PostgreSQL service so the isolation and pooled-connection-leakage tests actually run in CI, not just locally (marked TODO in `ci.yml`)
- [ ] Real `ResendEmailSender` once a Resend API key exists (D-04) — `LoggingEmailSender` stands in for now
- [ ] Rate limiting on auth endpoints — deliberately deferred to Phase 14 per `development-roadmap.md`'s own sequencing, not forgotten
- [ ] Session-listing/revoke-all could use direct JUnit coverage of `AuthServiceImpl` beyond the manual curl pass

## Phase 2 — Accounts and Categories

**Done:**
- [x] Found and fixed a critical gap: the runtime app connection (`neondb_owner`) had `BYPASSRLS` all along, which would have made every Phase 2+ RLS policy silently ineffective for real traffic. Created a restricted `app_runtime` role (`NOBYPASSRLS`), split runtime vs. migration credentials via `spring.flyway.user`/`spring.flyway.password` (`DATABASE_USER` vs `FLYWAY_DATABASE_USER`). Same gap still exists in the self-hosted Postgres path (`docker-compose.yml`) — not fixed yet since deploy is deferred, tracked below.
- [x] `V4__create_accounts_table.sql`, `V5__create_categories_table.sql` (18 seeded system categories per ADR-015 — corrected from the originally-documented 19; "Uncategorized" isn't a real row), `V6__create_merchants_table.sql` — all with RLS enabled and forced from the start
- [x] Fixed a real gap in `categories`' RLS: the single-policy pattern originally documented in `database-design.md` protects `SELECT` but not `UPDATE`/`DELETE` on system rows. Split into 4 command-scoped policies (select/insert/update/delete) so system categories are readable by everyone but writable by no one
- [x] Found and fixed a second RLS edge case: `current_setting(..., true)` can return `''` instead of `NULL` on a reused pooled connection, and `''::uuid` throws a hard error rather than matching zero rows. Added `NULLIF(..., '')` guard to every policy via `V7__fix_rls_policy_empty_string_guard.sql`; reproduced the raw error via direct psql before and after to confirm the fix
- [x] `Account`/`AccountService`/`AccountController` (CRUD + soft-deactivate, matching the project's never-hard-delete-financial-records stance), `Category`/`CategoryService`/`CategoryController` (custom categories only — system rows are read-only to users), `Merchant`/`MerchantService`/`MerchantController` with a basic normalization stub (lowercase, strip punctuation/whitespace) and duplicate-name rejection (409)
- [x] Extracted `CurrentUserGuard` once the "throw if unauthenticated" pattern hit 5 controllers — refactored `AuthController`/`UserController` to use it too
- [x] Verified end to end against the real Neon dev branch, three independent ways: (1) via the API with two real users, confirming Bob never sees Alice's accounts/categories/merchants; (2) via direct psql as `app_runtime` with no tenant context, confirming RLS-protected tables return zero rows rather than leaking everything; (3) via direct psql as Bob's context, confirming he cannot read Alice's account, and cannot UPDATE or DELETE a system category (`UPDATE 0`/`DELETE 0`) even though he can SELECT it

- [x] Automated integration test suite added, covering both Phase 1 and Phase 2: `AuthFlowIT` (register/login/duplicate/wrong-password/sessions/logout/revoke-all/password-reset/CSRF), `ResourceIsolationIT` (two-user isolation across accounts/categories/merchants, 404-not-403 on cross-tenant access), `RowLevelSecurityIT` (raw JDBC — pooled-connection leakage, the `''::uuid` regression, system-category write protection). Runs via Maven's `verify` phase (failsafe plugin, `*IT.java`), separate from the fast H2 `test` phase (surefire), against a new `it` Spring profile
- [x] CI now runs a real `postgres:16` service container and bootstraps the same restricted `app_runtime` role via a new reusable script (`scripts/init-db-roles.sql`) before running `mvn verify` — RLS/isolation behavior is now actually checked on every push, not just locally
- [x] Found and fixed two more real bugs while building this, both caught by testing against a real server rather than reading the code: (1) psql's `:'var'` substitution silently does not apply inside dollar-quoted `DO $$ ... $$` blocks — the role-bootstrap script's first version passed the literal text `:'password'` through uncaught; rewritten using `\gexec` instead. (2) JDK's `java.net.CookieManager` correctly refuses to resend a `Secure`-flagged cookie over the plain `http://localhost` the test server uses (no TLS in tests) — every IT test initially failed CSRF for this reason; `TestHttpClient` now tracks cookies manually instead, matching how curl (which ignores `Secure`) was used to hand-verify this project's auth/RLS behavior in the first place

**Left:**
- [ ] The same `BYPASSRLS`-role gap exists in `docker-compose.yml`'s self-hosted Postgres (single `POSTGRES_USER` used for everything) — `scripts/init-db-roles.sql` is written and reusable for this, but not yet wired into the Docker init path since deploy is still deferred

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
