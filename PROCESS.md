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

**Left — deferred until actual beta-readiness, needs your action then, not more code now:**
- [ ] Provision the DigitalOcean droplet + Spaces bucket (ADR-017) — requires your DigitalOcean account/payment method
- [ ] Register/point a real domain at the droplet — `APP_DOMAIN` is currently a placeholder (`example.com`); Caddy's automatic HTTPS is confirmed working correctly and will obtain a real cert the moment this points at a real, publicly-reachable domain
- [ ] Add deploy secrets to GitHub Actions (SSH key to the droplet, registry credentials) and add the actual deploy step to `ci.yml`
- [ ] First live deploy to production, completing the Phase 0 exit gate ("a push to main reaches production automatically")
- [ ] Nightly backup cron/systemd timer actually scheduled on the droplet (script is ready, not yet installed anywhere)

**Decision (interim, pre-beta), superseded same day: Railway → Render + Vercel.** Railway was connected and ready, but project creation failed with "Your trial has expired" (a billing block only the user can clear). The interim plan changed to **Render for the backend, Vercel for the frontend** — deploy itself deferred to the next day, but the code changes it needs were built and verified today:
- [x] `server.port=${PORT:8080}` so the backend binds to Render's assigned port
- [x] `security.cookie-samesite` (default `Lax`, unchanged) — both `SessionCookieFactory` and `CsrfTokenFilter` now read it; a split-origin deploy overrides it to `None` via `SECURITY_COOKIE_SAMESITE`
- [x] `app.cors.allowed-origin` + new `CorsConfig` (`infrastructure.web.common`) — empty/off by default, set to the Vercel frontend's exact origin for a split-origin deploy
- [x] `VITE_API_BASE_URL` build-time env var in `frontend/src/api/client.ts` — falls back to the existing relative `/api/v1` (dev proxy) unless set, in which case a split-origin build bakes in the backend's absolute Render URL
- [x] Verified: `mvn verify` (13 unit + 23 IT, unchanged and green), `npm run build`/`npm run lint` clean, and manually confirmed against the local dev server that defaults are unchanged (`SameSite=Lax`, no `Secure`, register still succeeds) and that a `SECURITY_COOKIE_SAMESITE=None` override correctly changes the cookie — see `DECISIONS.md` for the full writeup including the accepted CSRF-defense-layer tradeoff of `SameSite=None`

**Done — deployed live the same day, ahead of the original "tomorrow" plan:**
- [x] Render backend service (`spendwise-backend`, Docker runtime from `backend/Dockerfile`, `dockerContext: backend`) created and live at **https://spendwise-backend-6e3g.onrender.com** — env vars set: `SPRING_PROFILES_ACTIVE=prod`, `SESSION_TTL=30d`, `SESSION_IDLE_TIMEOUT=24h`, `SECURITY_COOKIE_SAMESITE=None`, `APP_CORS_ALLOWED_ORIGIN`/`APP_BASE_URL` (Vercel frontend URL), and `DATABASE_URL`/`DATABASE_USER`/`DATABASE_PASSWORD`/`FLYWAY_DATABASE_USER`/`FLYWAY_DATABASE_PASSWORD` (existing Neon dev branch — no new migration work, per the user's own instruction)
- [x] Vercel frontend project (`spendwise-frontend`, Vite framework, `rootDirectory: frontend`, git-linked to the repo for auto-deploy on push) live at **https://spendwise-frontend-sand.vercel.app** — deployment protection (on by default, would've blocked all access) disabled; `VITE_API_BASE_URL` set to the Render backend's `/api/v1`
- [x] Found and fixed two real bugs surfaced only by actually deploying (not caught by any local testing or curl reproduction) — see `DECISIONS.md`: (1) Vercel returned a raw 404 for `/login` and any other client-side route until a `frontend/vercel.json` SPA rewrite (`/(.*)` → `/index.html`) was added; (2) the CSRF double-submit design assumed `document.cookie` could read the CSRF cookie, which only ever holds same-origin — `document.cookie` on `vercel.app` can never read a cookie `onrender.com` set, regardless of `SameSite`/`Secure`. Fixed by having `CsrfTokenFilter` also echo the token as an `X-CSRF-Token` *response* header (exposed cross-origin via `CorsConfig`'s `exposedHeaders`), and having the frontend read that header instead of the cookie — works identically in both topologies now
- [x] Verified end to end against the live split-origin deployment, in a real browser: `/login` resolves correctly, login succeeds, session cookie (`Secure`, `HttpOnly`, `SameSite=None`) round-trips correctly, and the dashboard loads real data (confirmed with the user's own account, password reset directly in the dev database at the user's request during this session)
- [x] Test account (`render-deploy-check@example.com`) created during verification cleaned up from the dev database afterward

DigitalOcean remains the actual V1 production target once beta-ready — see `DECISIONS.md` and ADR-017's update note.

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
- [ ] Google OAuth ("Sign in with Google") as an additional login method — user explicitly wants this, but later, not now (D-19 in `docs/09-project/open-decisions.md`)

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

**Done:**
- [x] `V8__create_transactions_table.sql` (RLS enabled/forced from the start, `NULLIF(..., '')` guard built in from the start this time, not retrofitted), `V9__create_transaction_splits_table.sql`, `V10__create_transaction_category_allocations_view.sql` (`WITH (security_invoker = true)` — a Postgres view runs with its owner's, i.e. the privileged migration role's, permissions by default, which would otherwise bypass RLS entirely for anyone querying the view; verified via direct psql with two users before it was ever automated), `V11__create_idempotency_keys_table.sql`
- [x] `com.finance.transaction` package: `Transaction`/`TransactionSplit` entities, `TransactionType`/`TransactionSource`/`TransactionStatus`/`TransferKind` enums, `TransactionRepository`/`TransactionSplitRepository`, `TransactionSpecifications` (dynamic filtering via `JpaSpecificationExecutor` — date range, account, category, merchant, type, status, currency), `TransactionService`/`TransactionServiceImpl` (create/list/get/update/soft-delete, balanced-splits validation, currency-must-match-account validation), `TransferService`/`TransferServiceImpl` (atomic paired transactions for `CARD_PAYMENT`/other transfer kinds, direction validation), `TransactionController` (`POST/GET/PUT/DELETE /transactions`, `POST /transactions/transfer`)
- [x] Generic `IdempotencyService`/`IdempotencyServiceImpl` (interface + impl) backed by the RLS-protected `idempotency_keys` table — reusable across `POST /transactions`, `POST /transactions/transfer`, and Phase 6's future statement-confirm endpoint, not one-off per endpoint
- [x] Found and fixed two schema/entity mismatches while building this, both caught by actually running the test suite (see `DECISIONS.md`): the `key` column renamed to `idempotency_key` (H2 reserves `key`), and `@Lob` removed from `responseBody` in favor of an explicit `columnDefinition = "text"` (Hibernate schema validation failed — `@Lob` on a `String` defaults to Postgres's `oid` type, not the `TEXT` the migration creates)
- [x] Manually verified end to end via curl against scratch Postgres: create/list/update/soft-delete lifecycle, validation rejections (wrong transaction type on a plain create, currency mismatch, zero amount, unbalanced splits), balanced splits accepted, repeated `Idempotency-Key` returns the original response without duplicating, `CARD_PAYMENT` transfer creates an atomic paired `CARD_PAYMENT_OUT`/`CARD_PAYMENT_IN` with a shared `transferGroupId` and rejects the wrong direction
- [x] Automated `TransactionFlowIT` added (5 tests: CRUD lifecycle, validation rejections, balanced splits, idempotency replay, transfer atomicity + wrong-direction rejection) — required adding `TestHttpClient.postWithHeader(path, body, headerName, headerValue)` for the `Idempotency-Key` header
- [x] Automated the two Phase 3 findings that had only been manually verified: `ResourceIsolationIT` gained a two-user transaction isolation test (list/get/update/delete all correctly 404 cross-tenant), `RowLevelSecurityIT` gained a raw-JDBC regression test proving `transaction_category_allocations` applies RLS for the querying user rather than leaking via the view owner's bypass privileges
- [x] Verified: `mvn verify` — 16 IT tests (up from 14) + 1 unit test, all green against scratch Postgres (Colima + `postgres:16-alpine`, not Neon — IT tests insert/delete real rows and Neon dev is meant to be kept clean for manual poking)
- [x] Explicitly scoped **out**: one-sided-transfer review/flagging (matching an unpaired transfer leg) — documented in `TransferServiceImpl`'s javadoc and `DECISIONS.md` as a Phase 6 statement-import concern, since the manual transfer endpoint's request DTO always requires both account ids and therefore always creates an already-paired transaction

- [x] Committed and pushed (`3aa8d58`); CI green on both `backend` and `frontend` jobs (run `36175091663`)

**Left:**
- [ ] Clean up/decide fate of the scratch Postgres (Colima `scratch-pg` container, port 55433) now that Phase 3's IT suite passes against it — left running for now in case more Phase 3/4 local IT runs are needed soon

## Transactions — "Add several" (feature branch `feature/multi-transaction-entry`, from `main`)

**Done:**
- [x] "Add several" modal on the Transactions page: one date and account, many rows, blank rows ignored; frontend-only through the existing `POST /transactions`
- [x] Retry-safe partial failure: per-row `Idempotency-Key`, saved rows locked, failed rows show their own error, retry resubmits only unsaved rows
- [x] Verified in a real browser (golden path, partial failure plus retry, phone-width layout); frontend build clean

**Left:**
- [x] Merged to `main` and live (with a follow-up CORS fix for the `Idempotency-Key` header)

## Cross-cutting — Backend layering retrofit (domain/application/infrastructure)

**Done:**
- [x] Every feature (`account`, `category`, `merchant`, `user`, `auth`, `transaction`) restructured from a flat `com.finance.<feature>` package into `domain.<feature>` / `application.<feature>` / `infrastructure.persistence.<feature>` / `infrastructure.web.<feature>` — see `DECISIONS.md` and `docs/09-project/coding-standards.md`'s new "Layering" section for the full convention and rationale
- [x] Cross-cutting infra relocated: tenant context → `infrastructure.tenancy`, generic web plumbing (`ApiResponse`/`CurrentUserGuard`/`PageMeta`/`CsrfTokenFilter`/`GlobalExceptionHandler`) → `infrastructure.web.common`, the `ApiException` hierarchy + `ApiError`/`ErrorCode` → `application.exception`, idempotency → `infrastructure.idempotency`, the whole auth mechanism (sessions, password-reset tokens, token hashing/generation, cookies) → `infrastructure.security` as a single unit
- [x] Stale flat placeholder packages for not-yet-built phases (`ai`, `analytics`, `budget`, `goal`, `insight`, `recurring`, `statement`) deleted — they'll be created directly under the new convention when their phase starts
- [x] Done feature-by-feature (`category` piloted first) with a full `mvn compile` / `test-compile` / `verify` gate after every step — all 16 IT tests + 1 unit test green throughout and at the end, no SQL/schema/RLS changes

- [x] Committed and pushed (`78a19a5`); CI green on both `backend` and `frontend` jobs (run `36181375371`)

## Phase 4 — Analytics

**Done:**
- [x] Built directly under the new domain/application/infrastructure layering (the first feature to start there rather than being retrofitted into it): `domain.analytics` (`PeriodFigures`, `CurrencyExclusion`, `CategoryBreakdownEntry`, `MerchantBreakdownEntry`, `TrendPoint`, the framework-free `AnalyticsCalculator`, and the `AnalyticsRepository` port), `application.analytics` (`AnalyticsService`/`AnalyticsServiceImpl` - period validation, currency default/exclusion), `infrastructure.persistence.analytics` (`AnalyticsRepositoryImpl`, JdbcTemplate directly - no JPA entity, see `DECISIONS.md`), `infrastructure.web.analytics` (`AnalyticsController` + response DTOs)
- [x] `GET /analytics/monthly`, `/categories`, `/merchants`, `/trends` - all take required `from`/`to` (`LocalDate`) + optional `currency`, capped at 36 months apart (see `DECISIONS.md` for why no `month` param or default)
- [x] All formulas from `analytics-specification.md` implemented: expenses/income/savings/savings_rate, category breakdown (via split-aware allocation, refund nets against its own category), merchant breakdown, monthly trend series (zero-filled), currency scoping with `excludedCurrencies`/`excludedTransactionCount` never silently dropped
- [x] The six reconciliation invariants automated as pure unit tests (`AnalyticsCalculatorTest`, no database) against the required September worked-example fixture - all pass: category/merchant breakdown sum to expenses, trend series sums to the period total, split allocations sum to the transaction amount, income − expenses = savings, and a transfer group is equal in magnitude and contributes zero to both
- [x] Found and fixed a real bug while building the unit tests: `Map.of()`'s immutable maps throw `NullPointerException` on a null-key lookup (the "Uncategorized"/"Unknown merchant" bucket case) where `HashMap`/`LinkedHashMap` don't - fixed in `AnalyticsCalculator` by checking for the null id before touching the map, not by relying on a particular `Map` implementation
- [x] `AnalyticsServiceImplTest` (Mockito) covers period validation (>36 months, `to` before `from`) and currency resolution (default vs requested) without touching Postgres
- [x] `AnalyticsFlowIT` (6 tests) automates the worked example end to end through the real HTTP API against real Postgres - monthly summary, category/merchant breakdown, trend, currency exclusion, and the missing-period/over-36-months/backwards-range 400s
- [x] `ResourceIsolationIT` gained a two-user analytics isolation test (B's `/analytics/monthly` is unaffected by A's transactions)
- [x] `GlobalExceptionHandler` gained `MissingServletRequestParameterException`/`MethodArgumentTypeMismatchException` handlers - analytics is the first feature with required, typed query params, and both previously fell through to the generic 500 handler
- [x] Verified: `mvn compile`/`test-compile`/`verify` - 23 IT tests (up from 16) + unit tests all green against scratch Postgres

- [x] Committed and pushed (`ebfc987`); CI green on both `backend` and `frontend` jobs (run `36183995745`)

**Left:**
- [x] ~~Frontend dashboard wiring (S03)~~ — done; see the new "Frontend build" section below

## Phase 5 — Internal Dogfooding

**Done:**
- [x] Verified the backend boots cleanly against the real Neon dev branch (not scratch Postgres) and `/api/v1/health` responds - confirmed with a fresh `mvn spring-boot:run`, left running in the background on `localhost:8080` for daily use
- [x] Decided the dogfooding mechanism for this phase: direct API calls (curl), not a frontend - see `DECISIONS.md`
- [x] `scripts/spendwise-cli.sh` - a small curl wrapper handling the session cookie + CSRF header, so daily manual entry doesn't mean hand-crafting headers every call
- [x] Added Swagger UI (`/swagger-ui/index.html`) and OpenAPI JSON (`/v3/api-docs`) as a browser-based alternative to the CLI, with the CSRF header wired as an "Authorize" scheme - see `DECISIONS.md`. Disabled in prod. Verified: `mvn verify` green (23 IT tests), all 21 endpoints listed correctly
- [x] Found and fixed a real bug in `spendwise-cli.sh`: `csrf_token()`'s `grep ... >/dev/null | awk ...` silently returned empty under real bash (the script's own shebang), even though it looked fine when tested interactively in zsh - every mutating call was sending a blank CSRF header. See `DECISIONS.md` for the fix and the "verify the actual script file, not the line in whatever shell is open" lesson
- [x] `scripts/spendwise-demo.sh` - one-command end-to-end smoke flow (register/login → add account → log two transactions → check `/analytics/monthly` → list transactions), built on `spendwise-cli.sh`. Verified against real Neon dev with the CSRF fix in place; test data cleaned up afterward

**Left:**
- [ ] Track real personal spending via the API/frontend for ~1 week (manual entry only, no statement import)
- [ ] Fix whatever bugs surface from real use - this phase's only planned "work" per `phase-plan.md`
- [ ] Exit gate: the app is genuinely easier to reach for than whatever it replaces; if not, revisit the core loop before Phase 6

## Frontend build — real working UI for Phases 0-5

**Done:**
- [x] Built a real React 19 + Vite + React Router 7 + TypeScript (strict) frontend in `frontend/`, wired to the actual backend API (Vite dev proxy to `localhost:8080`, no CORS needed) — not a mockup; see `DECISIONS.md` for the shape decisions (single `apiClient` envelope/CSRF layer, `AuthContext`/`ReferenceDataContext`, register-then-login chaining)
- [x] Auth: login, register, logout, forgot-password (generic confirmation, no account enumeration), reset-password (`?token=` query param, invalid-link handling), route guards (`RequireAuth` redirects anonymous visits to `/login`; `RedirectIfAuthenticated` redirects an authenticated visit to `/login` or `/register` back to `/`)
- [x] Accounts: grid view, create/edit modal, deactivate (soft-delete, confirmed via `window.confirm`) — no "reactivate" UI, because the backend's `AccountController` has no reactivate endpoint (create/list/get/update/deactivate only); the frontend correctly reflects the API's actual capability, not a gap to fix
- [x] Transactions: filterable/paginated table (account/category/type/date-range filters), create/edit modal with inline merchant quick-create, delete (soft-delete, confirmed via `window.confirm`), and a separate transfer modal (`TRANSFER`/`CARD_PAYMENT` kinds) that creates the atomic paired transaction rows
- [x] Dashboard: period selector (this month / last month / last 3 months), summary cards (income/expenses/savings/savings rate), category breakdown bars, monthly trend chart, top merchants, recent transactions — all CSS-only (no charting library), all real computed figures from `/analytics/*`
- [x] Scoped out for time (see `DECISIONS.md`): transaction splits UI, free-text transaction search
- [x] Verified end to end in a real browser against the real Neon dev branch: register → auto-login → create accounts → create/edit a transaction with inline merchant quick-create → record a card-payment transfer (confirmed atomic pair in the transaction list) → dashboard figures update correctly → period selector refetches correctly → logout → both route guards redirect correctly → forgot/reset-password screens render and submit correctly. All UI-test users and their data deleted from the dev database afterward.
- [x] Verified: `npm run build` (tsc strict + vite build) and `npm run lint` (oxlint) both clean (lint has 3 pre-existing style warnings, no errors)
- [x] Found and fixed a real cross-browser bug surfaced by the user's own Safari testing: `SessionCookieFactory`/`CsrfTokenFilter` hardcoded `Secure` on their cookies, which Safari (unlike Chromium, which special-cases `localhost`) silently refuses to store over the Vite dev server's plain `http://localhost`, breaking login/CSRF entirely in that browser. Fixed via a new `security.cookie-secure` property (default `true`, overridden to `false` only in `application-dev.properties`) - see `DECISIONS.md`. Verified via curl (both cookies now lack `Secure` in dev, full register→login succeeds) and `mvn verify` (13 unit + 23 IT tests, unchanged and green, including `AuthFlowIT`'s `Secure`-cookie assertion for non-dev profiles)

## Phase 6 — Statement Import

**Standing workflow from here on (see `DECISIONS.md`):** built on a `staging` branch, not `main` — `main` is now a live deployment (Render + Vercel) the user actually uses, and only advances by merging `staging` in once a feature is verified complete. `staging` branched from `main` at `ea204db`.

**Done:**
- [x] `V12__create_statements_table.sql`, `V13__create_statement_transactions_table.sql` — RLS enabled/forced on both, matching every prior table's pattern; verified to apply cleanly against the real Neon dev branch, not just scratch Postgres
- [x] Full pipeline under the standard domain/application/infrastructure layering: `domain.statement` (`Statement`/`StatementTransaction` entities with explicit state-transition methods, `StatementStatus`/`ReviewStatus`/`DuplicateStatus` enums, `ParsedStatement`/`ParsedTransactionRow` value objects, repository ports), `application.statement` (`StatementService`/`StatementServiceImpl` — upload orchestration, review, confirm, retry), `infrastructure.pdf` (`PdfValidator`, `PdfTextExtractor`, `StatementFormatDetector`, `TransactionNormalizer`, and one `StatementParser` per bank/wallet — `HdfcBankStatementParser`, `SbiStatementParser`, `AxisBankStatementParser`, `BobStatementParser`, `UjjivanStatementParser`, `PaytmWalletStatementParser`, the last four sharing row-parsing logic via `StatementParsingSupport.parseSixColumnRows()`), `infrastructure.storage` (`StorageService` port + `LocalFilesystemStorageService`), `infrastructure.persistence.statement`, `infrastructure.web.statement` (`StatementController`, the 7 endpoints from `api-specification.md`)
- [x] Frontend: real `StatementsPage.tsx` (list, upload modal, a review screen with per-row inline-edit and confirm/retry actions) replacing the "Coming Soon" placeholder, added to sidebar nav — built after the user pointed out the original plan only covered the backend
- [x] ADR-014 corrected: the original three banks (HDFC, SBI, Axis) were a speculative guess made before real accounts existed; the user's actual accounts are Paytm Wallet, BOB, Ujjivan and Axis, so those three were added (HDFC/SBI kept, not removed) — now six supported banks/wallets total, matching the ADR's own "banks the builder actually holds accounts with" rationale
- [x] Scoped out for this pass, both confirmed with the user beforehand: OCR (native extraction only; a scanned PDF fails clean into `FAILED`; a cloud OCR API is the natural future path, not a local Tesseract install) and the DigitalOcean Spaces client (local filesystem storage only, matching `environments.md`'s documented dev setup; S3-compatible storage added once DO is actually provisioned)
- [x] `POST /statements/{id}/confirm` is idempotent two ways: the existing `IdempotencyService`/`Idempotency-Key` header (same mechanism as `POST /transactions/transfer`), and state-based — a repeat call against an already-`IMPORTED` statement returns the original result without re-importing, per `api-specification.md`'s explicit requirement
- [x] Found and fixed three real bugs, none caught by just reading the code (see `DECISIONS.md` for the full writeup): a classification-order bug in `TransactionNormalizer` where prefix-stripping ran before keyword matching, stripping the very keyword being matched on; missing `@Transactional` on `StatementServiceImpl`'s read methods (RLS silently zeroed every list/get); and a `createdAt: null` in the upload response caused by reading an entity before Hibernate's `@CreationTimestamp` flush had populated it
- [x] `TestHttpClient` gained multipart/form-data upload support (`postMultipart`) for the IT tests; new `PdfFixtures` test helper generates synthetic statement PDFs in-memory via PDFBox at test-run time (never committed binary files, never real financial data)
- [x] Verified: `mvn verify` — 47 unit tests (up from 13) + 30 IT tests (up from 23), all green. Full manual smoke tests via curl against the real Neon dev branch: the original HDFC-format flow (register → login → create account → upload → review → confirm), and separately Paytm Wallet/BOB/Ujjivan statements uploaded to the user's real accounts of those types, all parsing and classifying correctly. `npm run build`/`lint` clean. All test data cleaned up from the dev database and local filesystem storage afterward
- [x] **Verified**: the frontend's actual review UI in a live browser, using the real Paytm upload below — the Statements list, the per-row review table, dates/descriptions/amounts/expense-vs-income classification all rendering correctly against real data. The file-picker step itself was still done via a direct API upload (multipart curl), not a literal drag-and-drop through the browser — the built-in browser pane can't set a file input's value and Claude in Chrome wasn't connected this session — but every other part of the page, including the one that matters most (does it show the real parsed data correctly), is now verified in a live browser, not just via API responses
- [x] `PaytmWalletStatementParser` validated and fixed against the user's real Paytm UPI statement PDF, not just synthetic fixtures — found and fixed a real bug (a short, non-wrapping account name like "UPI Lite" shares its line with the amount instead of getting its own line, which the parser's amount-matching didn't account for and which silently misattributed amounts to the wrong transaction); see `DECISIONS.md` for the full root-cause writeup. Fixed parser's row/debit/credit totals now match the real statement's own printed summary exactly. `PaytmWalletStatementParserTest` rewritten to cover the real shapes this uncovered, using synthetic data only. Real statement file and its extracted text never committed or retained.
- [x] Found and fixed a **second** real bug the same file uncovered, one level up from the parser itself: `StatementFormatDetector` picked `BobStatementParser`/`UjjivanStatementParser` instead of Paytm's own parser, because Paytm's passbook labels a transaction's *linked* account "Bank Of Baroda"/"Ujjivan Small Finance Bank" and those parsers' `matches()` only checked for the bank's name as a substring, not whether the text actually had their six-column row shape. Fixed by adding `StatementParsingSupport.hasMatchingRow()` and requiring both signals. This bug only appears when going through the real detection pipeline (not when unit-testing or jshell-testing a parser in isolation) — see `DECISIONS.md` for the general lesson. Regression tests added to both parsers' test classes.
- [x] Ran the real app end to end for this: started the backend against the real Neon dev branch and the frontend dev server, uploaded the user's real Paytm PDF through the actual running `POST /statements/upload` endpoint (not just automated tests), and visually confirmed via the Statements review UI in a live browser that all 97 rows parsed correctly — 93 expenses (Rs.76,761.23) + 4 income rows (Rs.4,706), matching the statement's own printed totals exactly. Left as `READY_FOR_REVIEW`, not confirmed into real transactions — that's the user's call, not something to do unprompted mid-verification.

- [x] Description cleanup layer: `DescriptionCleaner` interface + `DescriptionCleanerRegistry` + `BobDescriptionCleaner` wired into staging (172/186 real BOB rows cleaned); BOB parser now joins wrapped narration without splitting handles. Cleaners for the other banks are added as their real statements arrive.

**Left:**
- [ ] Real-world parser accuracy for HDFC/SBI/Axis/BOB/Ujjivan against the user's actual bank statements — explicitly a Phase 8 ("Dogfood Statements") concern per `phase-plan.md`'s own framing; only Paytm Wallet has been validated against a real statement so far (see Done above), the rest are still only tested against synthetic fixtures. Worth specifically re-checking BOB/Ujjivan once real statements for those exist too, given the format-detection false-positive just found — their own real statements need to actually contain rows matching the six-column pattern `hasMatchingRow()` now requires, which hasn't been confirmed against real files for either
- [ ] Merchant/category auto-suggestion on staged rows (`suggestedMerchantId`/`suggestedCategoryId` are always null for now) — left to manual assignment during review; a reasonable candidate for Phase 11 (AI Categorization) rather than duplicating that effort here
- [ ] Rate limiting on the upload endpoint (`security.md` calls for it) — auth endpoints don't have it yet either (Phase 1's own "Left" list), so this isn't a new gap specific to Phase 6, just not yet addressed project-wide
- [ ] Real transfer-pairing for statement-imported `TRANSFER_OUT`/`TRANSFER_IN`/`CARD_PAYMENT_OUT`/`CARD_PAYMENT_IN` rows (matching/creating the counterpart transaction) — classified from narration keywords only for now; full pairing across accounts is a larger piece of work noted but not built in this pass
- [ ] Full duplicate-detection scoring (`duplicate_status` defaults to `UNKNOWN` for every staged row) — explicitly Phase 7 ("Normalization and Duplicates") scope, not Phase 6's
- [ ] Not yet merged to `main` — stays on `staging` until the user reviews and confirms it's ready

## Phase 7 — Normalization and Duplicates

**Done:**
- [x] Duplicate scoring (`DuplicateScorer` + `DuplicateDetectionService`): exact reference across all accounts, same-day amount+description, nearby-date similarity; one-to-one matching against imported transactions
- [x] `V14` migration; staged rows keep their payment reference; confirm stores raw narration + reference on the imported transaction
- [x] Duplicates skipped on confirm unless the user chooses "Import anyway" (audit timestamp stored); `POSSIBLE_DUPLICATE` flagged but imported; confirm reports how many were skipped
- [x] Re-scoring when the review screen opens (`/duplicates/recheck`) and again at confirm, so statements staged before an overlapping one was confirmed are still caught
- [x] Frontend: duplicate / possible-duplicate badges, "Import anyway", kept-anyway label, skipped-count message
- [x] BOB parser extracts the UPI reference; verified on real data (24/97 Paytm rows matched against the real BOB statement, equal to Paytm's own count)
- [x] "Remember my edits": merchant/category choices stored per payee (`payee_rules`, V15) and applied to sibling rows and later statements; staged rows now keep a stable order
- [x] Tests: 74 unit + 33 integration green

**Left:**
- [ ] Wider merchant-normalization rules (the free keyword dictionary covers well-known brands only)
- [ ] Transfer pairing / one-sided transfer review (still Phase 6 "Left")
- [x] Merged to `main` (live) together with the rest of the statement module, at the user's request


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
