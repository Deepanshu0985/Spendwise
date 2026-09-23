# Backend

Spring Boot (Maven) backend for the Personal Finance Intelligence App. See `../docs/` for the full spec.

## Prerequisites

This machine's default `java` on `PATH` is an old Java 8 browser-plugin runtime, which cannot run Spring Boot 3.x. The project is pinned to Homebrew's version-pinned `openjdk@21` (see `DECISIONS.md` — the generic `openjdk` formula silently drifted to JDK 27 and broke Lombok). Point `JAVA_HOME` at it explicitly:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
```

Add those two lines to `~/.zshrc` to avoid repeating them every session, or export them per-command as below.

## Run locally (no Docker)

The `dev` profile connects to a Neon Postgres branch (see `../DECISIONS.md` — "Neon Postgres replaces H2 for local development"), not a local database. Copy `../.env.example` to `../.env` and fill in `DATABASE_URL`/`DATABASE_USER`/`DATABASE_PASSWORD` from your Neon connection details, then load it into the shell before running:

```bash
set -a && source ../.env && set +a
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH" mvn spring-boot:run
```

Verify it's up:

```bash
curl http://localhost:8080/api/v1/health
```

## Test

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH" mvn test
```

Tests run against an in-memory H2 database (`test` profile) — fast, isolated, and never touching Neon. Anything Postgres-specific (Row-Level Security, from Phase 1 on) can't be meaningfully tested against H2 and needs a real Postgres run instead.

## Production

Production uses PostgreSQL via Docker Compose on the ADR-017 droplet, activated with `SPRING_PROFILES_ACTIVE=prod` — see `../docs/08-devops/deployment.md` and `../docs/08-devops/environments.md`. Nothing in this local setup affects that path; `application-prod.properties` is untouched.

## Layout

Package structure mirrors `../docs/02-architecture/lld.md`. Flyway migrations live in `src/main/resources/db/migration`; the first one (`V1__users.sql`, Phase 1) is not written yet, so the schema is currently empty and `ddl-auto=validate` trivially passes.
