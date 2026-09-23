# Decisions Log

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
