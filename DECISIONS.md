## Bug: `spendwise-cli.sh`'s CSRF token extraction silently returned empty under bash

**What happened.** `csrf_token()` read `grep -w csrf_token "$COOKIE_JAR" >/dev/null | awk '{print $7}'`. When I first verified this line, I ran it interactively in my own shell - which defaults to zsh - where that redirect-plus-pipe combination still happened to deliver grep's matched line to `awk`. I concluded it was harmless and told the user so. It isn't: the script's own shebang is `#!/usr/bin/env bash`, and under real bash, `cmd >/dev/null | next` sends `cmd`'s stdout only to `/dev/null` - `next` (`awk`) receives nothing. Every mutating call the script made (`register`, `login`, `post`, `put`, `delete`) therefore sent an empty `X-CSRF-Token` header, which `CsrfTokenFilter` correctly rejected with 403. It stayed hidden through an earlier round of testing because that check happened to only exercise a `GET` request - which never needs the CSRF header at all - so the broken code path was never actually run.

**Fix.** Removed the stray `>/dev/null` (`grep -w csrf_token "$COOKIE_JAR" | awk '{print $7}'`). Verified by executing the script's actual file (`bash scripts/spendwise-demo.sh ...`), not by re-testing the isolated line in an interactive shell - the whole end-to-end flow (register → login → add account → log transactions → check `/analytics/monthly` → list transactions) now succeeds.

**Lesson, worth remembering given it's easy to repeat:** verifying a shell snippet in an ad hoc interactive shell is not the same as verifying the script it lives in - the interactive shell and the script's shebang can be different shells (zsh vs. bash here) with genuinely different pipe/redirect semantics for the exact same-looking line. Always execute the actual script file to verify a fix, not a copy of the line typed into whatever shell happens to be open.

## Phase 5: added a `spendwise-demo.sh` end-to-end smoke script

**Decision.** `scripts/spendwise-demo.sh`, built on top of `spendwise-cli.sh`, runs the full loop in one command: log in (registering first if the account doesn't exist), add a bank account, log an expense and an income transaction, then check `/analytics/monthly` and list the new account's transactions. Not part of the deployed product - a convenience for quickly re-verifying the core loop still works, per the user's request for "a script other than the code" to add an account, log a few transactions, and check.

**Consequences.** Re-running it creates a fresh "Demo Bank" account and two more transactions each time - it doesn't dedupe against a previous run, since that would need to know which account is "the" demo account across runs. Verified end to end against the real Neon dev branch (with the CSRF fix above); test data created during verification was deleted afterward.

## Phase 5: added Swagger UI as a browser-based alternative to the curl CLI

**Decision.** `springdoc-openapi-starter-webmvc-ui` was added, exposing Swagger UI at `/swagger-ui/index.html` and the raw OpenAPI JSON at `/v3/api-docs`, generated from the existing controller annotations with no extra doc-comment burden. `OpenApiConfig` (`infrastructure.web.common`) declares the double-submit CSRF header (ADR-009) as an `apiKey` security scheme, so clicking "Authorize" and pasting the `csrf_token` cookie's value attaches `X-CSRF-Token` to every "Try it out" call automatically, matching what `CsrfTokenFilter` actually checks. Disabled in `application-prod.properties` (`springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`) - not meant to be publicly reachable once deployed.

**Why.** User request, as a better way to exercise the API during Phase 5 dogfooding than hand-built curl calls - a form-based UI with the request/response shapes already visible beats remembering every field name. This is additive tooling only: no application code changed, and the existing `scripts/spendwise-cli.sh` still works for anyone who prefers the terminal.

**Consequences.** Verified: `mvn compile`/`verify` green (23 IT tests unaffected), and `/v3/api-docs` correctly lists all 21 endpoint paths across every controller. To use it: open `/swagger-ui/index.html`, call `POST /auth/register` then `/auth/login` via "Try it out" (the browser's session cookie is sent automatically on same-origin requests even though it's httpOnly), then click "Authorize" and paste the `csrf_token` cookie's value (visible in the browser's dev tools) before trying any other mutating endpoint.

## Phase 5: dogfooding via direct API calls, frontend build stays deferred

**Decision.** For the internal-dogfooding soak week, the user tracks real personal spending by calling the backend API directly (curl/a REST client) against the real Neon dev branch, rather than having a minimal frontend built first.

**Why.** The user's explicit choice when asked: the frontend remains a skeleton (S01-S12 in `screen-specification.md` are all unbuilt beyond routing and a placeholder dashboard), and building even a minimal usable UI (login, accounts, add/view transactions, dashboard) would be genuinely new feature work - `phase-plan.md`'s own framing of Phase 5 is "none beyond bug fixes surfaced by real use." Keeping frontend deferred here is consistent with that and with the project's standing build-backend-first-through-all-phases approach.

**Consequences.** The backend was verified to boot cleanly against the real Neon dev branch (`mvn spring-boot:run`, `/api/v1/health` responds) and left running for daily use. Any bugs the user hits while using the app for real - via curl, not a UI - are this phase's actual work. Whether the exit gate ("genuinely easier to reach for than whatever it replaces") can be honestly judged through raw API calls, or whether a minimal frontend becomes necessary to judge it properly, is worth revisiting if the week doesn't produce a clear answer either way.

## Phase 4: analytics computed by a pure in-memory calculator, not SQL aggregation

**Decision.** `AnalyticsRepository` (the domain port) does the minimum possible in SQL - one JdbcTemplate query fetching every `CONFIRMED`, resolved-type transaction (with its splits) in a date range, currency-agnostic, plus two id→name lookup queries for categories/merchants. All of analytics-specification.md's actual math (expenses/income/savings/savings_rate, category and merchant breakdown with the refund-nets-against-its-own-bucket rule, the monthly trend series, zero-filling) lives in `AnalyticsCalculator` (`domain.analytics`), a framework-free static utility operating on plain `TransactionWithSplits` lists.

**Why.** Phase 3 already builds `Transaction`/`TransactionSplit`/`TransactionWithSplits` as plain domain objects with public reconstitution constructors - reusing them means the September worked example (and the six reconciliation invariants) can be built as in-memory fixtures and asserted with zero database, zero Spring context, in milliseconds (`AnalyticsCalculatorTest`, 7 tests). Doing the same math as native SQL `CASE`/`GROUP BY` would have made those invariants untestable except as slow, harder-to-debug integration tests, and duplicated the ledger-side classification rules (`analytics-specification.md`'s Transaction Types table) in two languages. This also matches the spec's own framing - "every number...computed deterministically by the ANALYTICS module" describes a module of code, not a SQL view.

**A real bug this caught immediately:** the first version of `AnalyticsCalculator.categoryBreakdown`/`merchantBreakdown` used `map.getOrDefault(nullKey, fallback)` to label the "Uncategorized"/"Unknown merchant" buckets. `Map.of(...)` (used by a unit test's fixture) throws `NullPointerException` on any lookup with a null key - `HashMap`/`LinkedHashMap` tolerate it, `Map.of()` doesn't. The production repository happens to return a `LinkedHashMap`, so this would never have surfaced against real Postgres, only in a future caller that (reasonably) built an immutable map. Fixed by checking for a null id before ever calling into the map, not by picking a particular `Map` implementation and hoping every caller uses the same one.

## Phase 4: `from`/`to` (LocalDate) on every analytics endpoint, not a `month` parameter or a default

**Decision.** All four endpoints (`/analytics/monthly`, `/categories`, `/merchants`, `/trends`) take required `from`/`to` `LocalDate` query params (plus optional `currency`), capped at 36 months apart - reusing the exact param shape `GET /transactions` already uses, rather than inventing a `YearMonth`-typed `month` parameter. `/monthly` and the breakdown endpoints return one aggregate over the whole range; `/trends` returns the same range grouped into one zero-filled point per calendar month.

**Why.** api-specification.md's line - "All analytics endpoints accept currency...and a period bounded to a maximum of 36 months" - applies the same period rule to all four, which only makes sense if "monthly" means the feature name (a period summary), not a literal single calendar month. Spring also doesn't support `YearMonth` as a `@RequestParam` type out of the box (`Jsr310DateTimeFormatAnnotationFormatterFactory` covers `LocalDate`/`LocalDateTime`/etc. but not `YearMonth`), so a `month` param would have needed a custom converter for no real benefit. No default is applied when `from`/`to` are omitted (they 400 instead) - computing "the current month" server-side would need a timezone reference, and `users.timezone` explicitly governs display/scheduling only, never transaction bucketing (Period Semantics) - the frontend already knows its own local "today" and is the right place to decide that.

**Consequence:** "month-over-month comparison" (a phase-plan.md task) isn't a distinct endpoint or a `previousMonth` field - two calls to `/monthly` for adjacent ranges, or two adjacent points from `/trends`, both already give it. Also required adding `MissingServletRequestParameterException` and `MethodArgumentTypeMismatchException` handlers to `GlobalExceptionHandler`: analytics is the first feature with required, typed `@RequestParam`s, and Spring throws those two before any controller code runs, which previously fell through to the generic 500 handler instead of the 400 the API contract requires.

## Phase 4: AnalyticsRepositoryImpl uses JdbcTemplate directly, not a JPA entity

**Decision.** `infrastructure.persistence.analytics.AnalyticsRepositoryImpl` queries `transactions`/`transaction_splits`/`categories`/`merchants` directly via `JdbcTemplate`, with no `AnalyticsJpaEntity` of any kind - a deliberate exception to this project's usual JPA-entity-per-table persistence pattern.

**Why.** There's no "analytics row" to persist - this is a pure read model over tables four other features already own as JPA entities. Reaching into `infrastructure.persistence.transaction`'s (or `category`'s/`merchant`'s) package-private JPA classes from `infrastructure.persistence.analytics` would leak one feature's persistence detail into another's adapter; querying the underlying tables directly with plain SQL keeps this feature's adapter self-contained. `user_id = ?` is still applied explicitly in every query, the same defense-in-depth-alongside-RLS convention every other repository in this project follows.

## Retrofit backend to domain/application/infrastructure layering

**Decision.** Every backend feature (`account`, `category`, `merchant`, `user`, `auth`, `transaction`) was restructured from a single flat `com.finance.<feature>` package (JPA entity, Spring Data repository, service interface + impl, controller, DTOs all side by side) into four package roots per feature: `com.finance.domain.<feature>` (framework-free business entity + enums + repository port interface), `com.finance.application.<feature>` (`Service`/`ServiceImpl` + command objects distinct from the web request DTOs), `com.finance.infrastructure.persistence.<feature>` (JPA entity, Spring Data repository, a mapper, and the port implementation), and `com.finance.infrastructure.web.<feature>` (controller + request/response DTOs). Cross-cutting infrastructure moved too: `TenantContext`/`CurrentUserGuard`/`ApiResponse`/`PageMeta`/`CsrfTokenFilter`/`GlobalExceptionHandler` → `infrastructure.tenancy` / `infrastructure.web.common`; the `ApiException` hierarchy + `ApiError`/`ErrorCode` → `application.exception`; idempotency → `infrastructure.idempotency`; the whole auth mechanism (`Session`, `PasswordResetToken`, token hashing/generation, cookies) → `infrastructure.security` as a single unit, since none of it is a personal-finance domain concept. `User` itself still got the full four-way split, since it is a genuine domain concept. See `docs/09-project/coding-standards.md`'s new "Layering" section for the complete convention and the worked `category` example.

**Why.** The user reviewed the file structure after Phase 3 and explicitly rejected the flat, package-by-feature layout as not "real and scalable" — given a choice between light layer-subfolders-within-each-feature and a full domain/application/infrastructure (hexagonal/clean architecture) split, the user chose the full split. This generalizes the project's existing SOLID/interface-first rule (`AccountService`/`AccountServiceImpl`, etc.) from the service layer to the whole codebase: business logic (domain), orchestration (application), and technical concerns (infrastructure: persistence, web, security) are now physically separated, so a future contributor can find "the business rule" without wading through JPA annotations and HTTP DTOs in the same file, and a future persistence-technology change would only touch the `infrastructure.persistence` layer.

**Two judgment calls worth recording, since they deviate slightly from a textbook-strict reading:**
1. **`Pageable`/`Page` are allowed in domain repository port signatures.** These are Spring Data *Commons* pagination types, not JPA-specific — reinventing a parallel domain pagination abstraction would be pure ceremony at this project's scale, so this is an accepted, documented exception rather than an oversight.
2. **Not every technical interface+impl needs an infrastructure adapter.** `MerchantNormalizer`/`BasicMerchantNormalizer` (pure string algorithm, no I/O) lives entirely in `domain.merchant` — the "needs a real adapter" test is whether the implementation touches a database, the network, or the filesystem, not whether it's `@Component`-annotated.

**Consequences.** No SQL/schema/RLS changes — this is purely a Java package/class reorganization; Flyway migrations and table/column names are untouched. The existing 16 IT tests + 1 unit test were the regression net throughout: the retrofit was done and verified feature-by-feature (`category` piloted first, then `merchant` → `account` → `user`/`auth` → `transaction`, then the cross-cutting infra pass), running `mvn compile`/`test-compile`/`verify` after every step, all green at every stage and at the end. The flat placeholder packages for not-yet-built phases (`ai`, `analytics`, `budget`, `goal`, `insight`, `recurring`, `statement` — each just a one-line `package-info.java`) were deleted rather than left stale; they'll be created directly under the new convention when their phase actually starts. This is now the standing convention for Phase 4 onward, not a one-time cleanup.

## Phase 3: codified the two RLS/view findings as automated regression tests, not just manual psql checks

**Decision.** Added `secondUserNeverSeesOrCanModifyTheFirstUsersTransactions()` to `ResourceIsolationIT` (API-level, two real users, same 404-not-403 assertions already used for accounts) and `transactionCategoryAllocationsViewAppliesRlsForTheQueryingUserNotTheViewOwner()` to `RowLevelSecurityIT` (raw JDBC: insert a transaction as user A, then query the view under user B's `app.current_user_id` context and assert zero rows).

**Why.** Both of these were only ever verified by hand during Phase 3 development - two-curl-session isolation testing for the new `/transactions` endpoints, and direct psql testing that `transaction_category_allocations` actually respects `WITH (security_invoker = true)` rather than leaking every tenant's rows via the view owner's (privileged migration role's) bypass privileges. This project's own established pattern (Phase 1/2) is to always automate what was manually verified rather than leave it as a one-time check - a future migration that drops `security_invoker` from the view, or a future endpoint that forgets tenant scoping, would otherwise not be caught until it reached production.

**Consequences.** `mvn verify` now runs 16 IT tests (up from 14), all green against scratch Postgres. No production code changed - this is coverage added after Phase 3's transaction system was already functionally complete.

## Phase 3: idempotency table's `key` column renamed to `idempotency_key`, and `@Lob` removed from the response-body field

**What happened.** Two schema-vs-entity mismatches, both caught by actually running the test suite rather than by reading the code: (1) `IdempotencyKeyRecord`'s `@Column private String key` produced a Postgres `CREATE TABLE` with an unquoted `key` column, which H2 (used by `*Test.java`) rejects outright as a reserved word - the migration and entity had to agree on `idempotency_key` instead, keeping the Java field/getter named `key` since that's an internal implementation detail, not the wire/DB name. (2) `@Lob @Column(name="response_body") private String responseBody` mapped to Postgres's `oid` large-object type by default, but `V11__create_idempotency_keys_table.sql` declares the column as `TEXT` - Hibernate's schema validation (`ddl-auto=validate`) failed at boot with a type mismatch.

**Fix.** Migration and entity both use `idempotency_key` (`V11__create_idempotency_keys_table.sql`, `IdempotencyKeyRecord.java`). `@Lob` was removed in favor of `@Column(name = "response_body", nullable = false, columnDefinition = "text")`, which pins Hibernate to exactly what the migration creates instead of taking JPA's default type mapping for `String`.

## Phase 3: idempotency built as a generic, reusable mechanism, not one-off per endpoint

**Decision.** `IdempotencyService`/`IdempotencyServiceImpl` (interface + impl, per the project's SOLID rule) expose a single `executeIdempotent(userId, idempotencyKey, endpoint, successStatus, supplier)` method that stores the full original response keyed on `(user_id, idempotency_key, endpoint)` and replays it verbatim on a repeat. Both `POST /transactions` and `POST /transactions/transfer` use it via the same `Idempotency-Key` request header.

**Why.** `api-specification.md`'s Idempotency section applies to more than one endpoint (statement-confirm in Phase 6 will need it too) - building it generically once, backed by its own RLS-protected table (`idempotency_keys`), means every future mutating endpoint that needs idempotency opts in with one line rather than reimplementing key storage and replay logic per endpoint.

## Phase 3: manual transfer endpoint explicitly does not do one-sided-transfer flagging

**Decision.** `TransferServiceImpl`'s javadoc records that one-sided-transfer detection/flagging (matching an unpaired `TRANSFER_OUT`/`TRANSFER_IN` that arrived from two different statement imports) is out of scope for `POST /transactions/transfer`.

**Why.** That endpoint's `CreateTransferRequest` requires both `fromAccountId` and `toAccountId` in a single call, so it always creates an atomic, already-paired transaction (verified by the `cardPaymentTransferCreatesAnAtomicPairAndRejectsTheWrongDirection` IT test). The one-sided case phase-plan.md's Phase 3 scope note actually describes only arises from Phase 6's statement-import path, where each side of a transfer can land as a separate, unmatched row from two different bank statements imported independently. Recording this now so it isn't mistakenly treated as missing Phase 3 work later.

## Automated test suite: failsafe + a real `it` profile, not just more manual curl/psql passes

**Decision.** `*Test.java` (Maven's `test` phase, surefire) stay on H2 - fast, no external dependency. `*IT.java` (Maven's `verify` phase, failsafe - added as a new plugin) run against a real PostgreSQL instance via a new `it` Spring profile, which pins `spring.datasource.hikari.maximum-pool-size=1` so the pooled-connection leakage test is actually exercising what it claims to, not hoping the pool happens to reuse a connection.

**Why.** Everything in Phase 1 and 2 had only ever been verified by hand (curl, direct psql) - real verification, but not repeatable and not run on every push. RLS-dependent behavior specifically cannot be tested on H2 at all (ADR-018), so this needed real Postgres from the start, not a "fake it with H2" compromise.

**Two more bugs found while building this, both only surfaced by actually running the tests, not by reading the code:**

1. **psql doesn't substitute `:'var'` inside dollar-quoted `DO $$ ... $$` blocks.** `scripts/init-db-roles.sql`'s first version silently passed the literal text `:'password'` through to `CREATE ROLE ... PASSWORD :'password'`, which is a syntax error - caught immediately by running the script against a scratch Postgres before trusting it in CI. Fixed by moving the `CREATE ROLE` out of a `DO` block entirely, using `SELECT format(...) WHERE NOT EXISTS (...) \gexec` instead - `format(...)`'s `%L` does the safe literal-quoting, `\gexec` makes the conditional idempotent, and the substitution happens in an ordinary (non-dollar-quoted) statement where psql's `:'var'` actually works.

2. **JDK's `CookieManager` won't resend a `Secure` cookie over plain `http://localhost`.** Every single IT test failed CSRF validation on the first run - not because CSRF was broken, but because `TestHttpClient`'s original `java.net.CookieManager`-based cookie handling correctly refused to send the (correctly) `Secure`-flagged `csrf_token`/session cookies back over the test server's non-TLS connection. This is right behavior for a real browser in production and exactly wrong for a test harness with no TLS. Fixed by having `TestHttpClient` track cookies manually (parse `Set-Cookie`, resend as a plain `Cookie` header, ignoring the `Secure` flag) - the same thing curl was already doing, which is why the original hand-verification never hit this.

**Consequences.** `mvn verify` (already the command CI ran, even before this) now requires a real Postgres to succeed - the CI Postgres service and role bootstrap below were added in the same change, not a follow-up, specifically because merely adding the failsafe plugin would otherwise break the next CI run.

## CI PostgreSQL service, wired in the same change as the failsafe plugin

**Decision.** `.github/workflows/ci.yml`'s `backend` job now runs a `postgres:16` service container, bootstraps the restricted `app_runtime` role via `scripts/init-db-roles.sql` (a throwaway CI-only password, fine since the whole container is destroyed at the end of the job), then runs the same `mvn -B verify` command as before.

**Why.** `scripts/init-db-roles.sql` is written once and reused for CI now and the self-hosted Postgres prod path later (still not wired there - deploy is deferred), rather than duplicating the role-creation SQL inline in the workflow YAML and in DECISIONS.md's original manual Neon setup.

**Consequences.** RLS and pooled-connection-leakage behavior is now checked on every push, not only when someone happens to run it locally against Neon. The frontend job is unaffected.

## Found: the runtime app connection had BYPASSRLS all along

**What happened.** Before writing Phase 2's first RLS-protected tables, I checked whether Neon's default `neondb_owner` role - the one `DATABASE_USER` had used for everything since Phase 1 - has the `BYPASSRLS` role attribute. It does (`rolbypassrls = t`). ADR-010 already documented the intended shape of this ("migrations run under a role with BYPASSRLS," implying the runtime role should not) but it had never actually been implemented - Phase 1 didn't need it (ADR-019 excludes those three tables from RLS entirely), so the gap was latent and harmless until now. Had this shipped unnoticed, every RLS policy from Phase 2 onward would have been silently ineffective for real application traffic: `FORCE ROW LEVEL SECURITY` does nothing against a role with `BYPASSRLS`, regardless of ownership.

**Fix.** Created a second Postgres role, `app_runtime`, explicitly `NOBYPASSRLS`, granted `SELECT/INSERT/UPDATE/DELETE` on all tables plus default privileges so future migrations' tables are covered automatically:
```sql
CREATE ROLE app_runtime WITH LOGIN PASSWORD '...' NOBYPASSRLS NOSUPERUSER NOCREATEDB NOCREATEROLE;
GRANT USAGE ON SCHEMA public TO app_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_runtime;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_runtime;
```
`DATABASE_USER`/`DATABASE_PASSWORD` (the runtime `spring.datasource.*` credentials) now point to `app_runtime`; `FLYWAY_DATABASE_USER`/`FLYWAY_DATABASE_PASSWORD` (new) point to the privileged `neondb_owner` and are wired to `spring.flyway.user`/`spring.flyway.password` in `application.properties`, which Spring Boot's Flyway auto-configuration supports natively as an override distinct from the main datasource - exactly the "migrations use a different, more privileged user" scenario it's designed for. Verified: the app boots and passes health checks with this split in place before any RLS policy exists to test against; the actual RLS-bypass-proof verification happens once Phase 2's tables exist.

**Still open:** the self-hosted PostgreSQL container in `docker-compose.yml` (staging/production) has the same single-role gap - `POSTGRES_USER` is used for everything there too. Not fixed yet since deploy is deferred (see the build-locally-first decision), but tracked in `PROCESS.md` so it isn't forgotten by the time it matters.

## Backend loads its own .env instead of depending on the shell having sourced it

**What happened.** Running the app from IntelliJ's Run button failed with `Driver org.postgresql.Driver claims to not accept jdbcUrl, ${DATABASE_URL}` - the literal, unresolved placeholder text was passed as the JDBC URL. Root cause: `application-dev.properties` referenced `${DATABASE_URL}` expecting it to already be a process environment variable, which only became true because our documented workflow said to run `source .env` in the shell first. IntelliJ's Run button launches the JVM directly - there is no shell in that path to source anything into, so the environment variable genuinely never existed.

**Fix.** `application-dev.properties` now declares `spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]` - Spring Boot's own extension-hint bracket syntax, which loads the `.env` file's `KEY=VALUE` lines as properties directly, regardless of how the JVM was launched or what shell (if any) started it. Both a `backend/`-relative and a project-root-relative path are tried, both `optional:`, since IntelliJ and `mvn` can default to different working directories and whichever one doesn't match just silently no-ops. Verified by launching with `DATABASE_URL`/`DATABASE_USER`/`DATABASE_PASSWORD` explicitly unset from both working directories - both connect to the real Neon dev branch correctly. `backend/README.md`'s manual `source ../.env` step is removed as no longer necessary.

## Bug: blank .env values silently defeat Spring's `${VAR:default}` fallback

**What happened.** `application.properties` declares `session.ttl=${SESSION_TTL:30d}`. `.env` had `SESSION_TTL=` (blank). `source .env` exports `SESSION_TTL` as an empty string - a defined variable, not an absent one - so Spring's placeholder resolver never falls back to `30d`; it resolves to an empty string, which fails to bind to `Duration` and produces `null`. This surfaced as a `NullPointerException` in `SessionStoreImpl.create()` calling `Instant.plus(null)`, only when actually logging in (the earlier register/duplicate/wrong-password calls never touched `sessionTtl`).

**Fix.** Every variable actually consumed locally now has a real, non-blank value in both `.env` and `.env.example` (`SESSION_TTL=30d`, `SESSION_IDLE_TIMEOUT=24h`, `APP_BASE_URL=http://localhost:5173`). `.env.example` carries a comment explaining the gotcha so it isn't rediscovered the hard way again. Variables that are genuinely fine unset for now (STORAGE_*, AI_*, MAIL_API_KEY) stay blank since nothing reads them yet.

## Filters can't be caught by @RestControllerAdvice

**What happened.** `CsrfTokenFilter` threw `ForbiddenException` on a missing/invalid CSRF token, expecting `GlobalExceptionHandler` to turn it into a clean 403 envelope. Instead the client got a generic Spring Boot whitelabel 500. Root cause: servlet `Filter`s run before `DispatcherServlet`, so exceptions thrown from them never reach Spring MVC's `@ExceptionHandler` machinery at all - that only intercepts exceptions from controller method execution.

**Fix.** `CsrfTokenFilter` now catches this itself and writes the `ApiError` envelope directly to the response (status code, content type, serialized JSON via the injected `ObjectMapper`) instead of throwing. Verified via curl: a request with no/mismatched CSRF header now gets a proper `{"error":{"code":"FORBIDDEN",...}}` body with `HTTP 403`, not a whitelabel page. Any future filter that needs to reject a request must do the same - it cannot rely on `GlobalExceptionHandler`.

## Neon Postgres replaces H2 for local development (partially supersedes ADR-018)

**Decision.** The `dev` Spring profile points at a Neon Postgres branch instead of file-based H2. A dedicated Neon branch separate from `production` is used for local/dev work, never the production branch. `test` continues to use in-memory H2 for fast pure-logic unit tests that don't depend on Postgres-specific behavior.

**Why.** Phase 1 introduces Row-Level Security, which is meaningless against H2 (no equivalent feature) — every previous local run of an RLS or cross-tenant isolation test would have had to hit real PostgreSQL anyway, per ADR-018's own stated consequence. Neon removes the friction ADR-018 was originally trying to avoid (installing/running Docker + Postgres locally) while still being real PostgreSQL, so it satisfies the original no-Docker-friction goal at least as well as H2 did, without H2's RLS gap. Using a separate dev branch (not `production`) keeps schema migrations and RLS policy work, which Phase 1 is full of, off anything labeled production while the project has no real users yet.

**Consequences.** ADR-018 is not fully reversed — Docker is still not used for local development — but its "local dev = H2" specifics are superseded for the `dev` profile from Phase 1 onward. `docs/02-architecture/architecture-decisions.md` and `docs/08-devops/environments.md` are updated in the same change. The Neon connection string (with a live password) must never be committed; it lives only in a local, git-ignored `.env` (see `.env.example` for the template) that `backend/README.md` explains how to load.

The connection given is for the Neon branch literally named `production`, used directly per explicit user instruction rather than a separate dev branch. Since the project has no real users or deployed data yet, this branch functions as the dev database for now - worth remembering to revisit before anything real depends on it.

## Baselined Flyway on the Neon prod branch instead of dropping its pre-existing demo tables

**Decision.** `spring.flyway.baseline-on-migrate=true` was set (temporarily, while this branch was still the active `dev` target) after the Neon `production` branch turned out to have 4 pre-existing tables unrelated to this project - `account`, `customers`, `scheduled_transfer`, `transaction` (singular, Neon's own onboarding sample data, a handful of rows each) - which made Flyway refuse to run against a "non-empty schema with no history table." These tables were left in place rather than dropped, since this branch is designated for prod (see below) and no name collision with our schema exists (ours uses `accounts`/`transactions`, plural).

**Why.** User's explicit choice between dropping them and baselining around them, at the time this branch was the only one available. Verified end to end at the time: `mvn spring-boot:run` against this branch baselined cleanly (`Successfully baselined schema with version: 1`) and `/api/v1/health` responded. This branch is not actively used for `dev` anymore (see the dev/prod split below) but stays baselined and ready for when deploy actually happens.

## Split Neon into separate dev and prod branches; dev branch's demo tables dropped instead of baselined

**Decision.** A second Neon branch (`ep-super-wildflower-adhdau9g-pooler...`) is now the `dev` target; the original branch named `production` (`ep-gentle-night-adqd910m-pooler...`, baselined above) is designated for prod, not used yet since deploy is deferred. Unlike the prod branch, this dev branch's pre-existing objects - a *different* set of demo tables (`delivery_partners`, `order_audit_logs`, `orders`, and critically a `users` table with an incompatible bigint-id schema that directly collides with our own planned `users` table) plus two further orphaned sequences (`course_seq`, `learner_seq`) left over from yet another past experiment on the same Neon project - were dropped outright rather than baselined.

**Why.** User's explicit choice, and the only workable one here: baselining doesn't help when the colliding object has the *same name* as ours (`users`) - our own `V1__users.sql` migration would still fail trying to create a table that already exists with an incompatible schema. All dropped data was empty or trivial demo content (0-4 rows per table), confirmed before dropping. This is the second time a fresh-looking Neon branch has turned out to carry leftover objects from prior unrelated work on the same Neon project - worth checking `\dt public.*` (and, as this case showed, `pg_class` more broadly for orphaned sequences/views that don't show up in `\dt`) before trusting a "new" branch is actually empty.

**Consequences.** `application-dev.properties` no longer sets `baseline-on-migrate` (removed - the dev branch is genuinely empty, and leaving it on unnecessarily could mask a real future schema-drift problem instead of surfacing it). The real Neon credentials for both branches live only in the local, git-ignored `.env` (dev as `DATABASE_URL`/`DATABASE_USER`/`DATABASE_PASSWORD`, prod as `PROD_DATABASE_URL`/`PROD_DATABASE_USER`/`PROD_DATABASE_PASSWORD` for now, unused until deploy). Verified end to end against the cleaned dev branch: clean boot, Flyway creates its schema history table with no baseline needed, `/api/v1/health` responds.

## Build the whole product locally first; DigitalOcean + domain provisioning deferred to the end

**Decision.** Deployment (provisioning the ADR-017 droplet, pointing the domain, adding deploy secrets to CI, first live deploy) is deliberately deferred until the rest of the build is further along, rather than deploying incrementally from Phase 0. `ci.yml` runs build/test only, with no deploy step, for now.

**Why.** User-stated preference: build and verify everything locally through the phases, then provision and deploy once there's something substantial to put on the droplet. This doesn't change any of the deployment artifacts themselves — `docker-compose.yml`, both `Dockerfile`s, and the `Caddyfile` are already written and were validated locally with Colima before this decision, so they're ready to use whenever deployment actually happens; only the timing of provisioning real infrastructure changes.

## Frontend served as its own container by Caddy, not written directly onto the VPS disk

**Decision.** The frontend build produces its own Docker image (`frontend/Dockerfile`, multi-stage: Node build → `caddy:2-alpine` serving the static output), rather than running `npm run build` on the VPS host and bind-mounting the result into the edge Caddy container.

**Why.** `deployment.md`'s deploy pipeline is "build image → push to registry → ssh to host → docker compose pull && up -d" — everything that runs in production arrives as a pulled image, nothing is built on the host. Keeping the frontend consistent with that means the deploy pipeline has one shape for both frontend and backend, and a rollback (redeploy the previous image tag) works identically for either.

## Validated the Docker Compose deployment artifacts locally with Colima, then removed it

**Decision.** Installed Colima (a lightweight Docker runtime, not Docker Desktop) specifically to build both images and smoke-test the full compose stack — Caddy, backend, frontend, PostgreSQL — before considering Phase 0's deployment artifacts done. Removed Colima afterward.

**Why.** Per the standing rule to build and check for errors before calling anything finished, authoring `docker-compose.yml`/`Caddyfile`/both `Dockerfile`s without ever running them would mean shipping untested config to the first real deploy. The smoke test caught nothing wrong, but it did confirm: both images build, all four containers reach a healthy state, the backend connects to real PostgreSQL and runs Flyway against it (not H2), and Caddy's reverse-proxy routing to `backend:8080` and `frontend:80` both resolve correctly over the compose network. The one expected failure — Let's Encrypt refusing to issue a certificate for the placeholder `example.com` domain in `.env.example` — is not a config bug; it will resolve the moment `APP_DOMAIN` points at a real, publicly-reachable domain. Colima was removed afterward because local day-to-day development still doesn't use Docker (ADR-018) — this was a one-time verification of a deployment artifact, not a change to the dev workflow.

## CI runs backend + frontend build/test now; PostgreSQL service deferred to Phase 1

**Decision.** `.github/workflows/ci.yml` runs `mvn verify` (against H2, per the current test setup) and the frontend lint/build. It does not yet include a PostgreSQL service container.

**Why.** ADR-018 says PostgreSQL-dependent tests (Row-Level Security isolation, pooled-connection leakage) must run against real PostgreSQL in CI — but no such tests exist yet, since Phase 1 hasn't started. Adding a Postgres service now would validate nothing and just be dead weight in the workflow. A comment in the workflow file marks exactly where to add it once Phase 1 introduces those tests.

## SOLID is mandatory; every service is an interface plus an implementation

**Decision.** From Phase 1 onward, every service class a controller or another service depends on is split into an interface and an implementation (e.g. `AccountService` / `AccountServiceImpl`), wired by constructor injection against the interface type. This is checked in review alongside correctness and authorization, not treated as optional polish.

**Why.** User-stated standing requirement. It also generalizes the pattern `docs/02-architecture/lld.md` already uses for infrastructure ports (`StatementParser`, `FinanceTool`, `AIModelClient`, etc.) to the service layer, which pays for itself specifically in this project: Phase 6 needs multiple `StatementParser` implementations (HDFC, SBI, Axis per ADR-014) behind one interface, and Phase 11-12's AI layer needs `AIModelClient` swappable once D-03 is decided — designing every service this way from the start avoids retrofitting it under pressure later. Recorded as a standing rule in `docs/09-project/coding-standards.md`.

Implementation-level decisions made while building, with the reasoning behind each. This is a running log, most recent first. It sits alongside — not instead of — `docs/02-architecture/architecture-decisions.md` (the formal, numbered ADRs for product/system architecture) and `docs/09-project/open-decisions.md` (decisions not yet made). This file exists for the smaller, day-to-day build choices that still deserve a written reason, so nothing is re-litigated or forgotten later.

## Pin the JDK to a versioned Homebrew formula (openjdk@21), not the generic `openjdk` formula

**Decision.** Local backend development and IntelliJ both use `/opt/homebrew/opt/openjdk@21`, the version-pinned Homebrew formula, as `JAVA_HOME`.

**Why.** The generic `openjdk` formula tracks whatever Homebrew currently considers latest with no version pin — it was JDK 25 when Phase 0 started and had become JDK 27 a few days later with no explicit upgrade action taken. Spring Boot 3.3.5's managed Lombok version doesn't yet support JDK 27's `javac` internals, which surfaced as `java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN` — a crash in Lombok's javac-patching hook, not a bug in the project's own code. IntelliJ additionally had an SDK entry literally named "21" whose actual `homePath` resolved to the JDK 27 install, so the mislabeling hid the real cause until `jdk.table.xml` was inspected directly. Pinning to `openjdk@21` avoids both problems: the version can't silently drift, and it matches `<java.version>21</java.version>` in `pom.xml`.

## Build error-handling foundation in Phase 0, not deferred to later phases

**Decision.** `ApiResponse`, `ApiError`, `GlobalExceptionHandler`, and the `ApiException` hierarchy (`NotFoundException`, `ConflictException`, `DomainValidationException`) were built as part of the initial scaffold.

**Why.** Every module from Phase 1 onward returns errors through this contract (`docs/03-api/error-contract.md`). Building it once, correctly, before any controller exists avoids each phase inventing its own error shape and then needing a retrofit.

## Flyway enabled in dev against H2, even with zero migrations

**Decision.** `spring.flyway.enabled=true` in every profile, including `dev`, from the very first commit — not turned on only once the first migration is written.

**Why.** If Flyway is wired in from the start, the exact same migration history runs against H2 locally and PostgreSQL in production. Turning it on later risks a dev environment whose schema was built by some other means (e.g. Hibernate `ddl-auto=update`) quietly diverging from what migrations actually produce.

## `ddl-auto=validate`, never `update`, in every profile

**Decision.** Hibernate only validates the schema against the entities; it never creates or alters tables in any environment.

**Why.** Matches the project's own migrations-are-the-source-of-truth stance (`docs/08-devops/deployment.md`, additive-only migrations). If Hibernate were allowed to auto-generate schema in dev, a developer could ship code whose implicit schema assumptions were never actually captured in a migration file, which breaks the moment it hits a real database.

## No Docker for local development (ADR-018)

**Decision.** The backend runs directly via Maven against a file-based H2 database locally. Docker Compose and PostgreSQL remain the target for staging and production only.

**Why.** User preference for the fastest possible local edit-run loop during a solo build; recorded formally as ADR-018 in `docs/02-architecture/architecture-decisions.md` since it's consequential enough to affect testing strategy (Row-Level Security has no H2 equivalent, so isolation tests must run against real PostgreSQL in CI).
