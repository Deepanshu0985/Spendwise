# Backend

Spring Boot (Maven) backend for the Personal Finance Intelligence App. See `../docs/` for the full spec.

## Prerequisites

This machine's default `java` on `PATH` is an old Java 8 browser-plugin runtime, which cannot run Spring Boot 3.x. A newer JDK is installed via Homebrew (`brew install openjdk`) but not linked onto `PATH`, so point `JAVA_HOME` at it explicitly:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk
export PATH="$JAVA_HOME/bin:$PATH"
```

Add those two lines to `~/.zshrc` to avoid repeating them every session, or export them per-command as below.

## Run locally (no Docker)

Uses a file-based H2 database at `backend/data/` — nothing else to install or start.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk PATH="/opt/homebrew/opt/openjdk/bin:$PATH" mvn spring-boot:run
```

Verify it's up:

```bash
curl http://localhost:8080/api/v1/health
```

H2 console (browse the local dev database): http://localhost:8080/h2-console — JDBC URL `jdbc:h2:file:./data/financedb`, user `sa`, empty password.

Delete `backend/data/` to reset the local database from scratch.

## Test

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk PATH="/opt/homebrew/opt/openjdk/bin:$PATH" mvn test
```

Tests run against an in-memory H2 database (`test` profile), never the dev file database.

## Production

Production uses PostgreSQL via Docker Compose on the ADR-017 droplet, activated with `SPRING_PROFILES_ACTIVE=prod` — see `../docs/08-devops/deployment.md` and `../docs/08-devops/environments.md`. Nothing in this local setup affects that path; `application-prod.properties` is untouched.

## Layout

Package structure mirrors `../docs/02-architecture/lld.md`. Flyway migrations live in `src/main/resources/db/migration`; the first one (`V1__users.sql`, Phase 1) is not written yet, so the schema is currently empty and `ddl-auto=validate` trivially passes.
