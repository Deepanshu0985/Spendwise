# Phase Plan

Detailed breakdown of `development-roadmap.md` for a single full-time developer, with durations, dependencies and exit gates in one place. Re-estimate a phase here if its scope changes; do not let this drift from the roadmap's ordering rationale.

Estimates assume the decisions in `open-decisions.md` are used as scoped: three parser formats (ADR-014), the flat category set (ADR-015), Resend (ADR-016) and the DigitalOcean Bangalore droplet (ADR-017). A duration grows if any of those scopes grow.

## Summary

| Phase | Name | Duration | Depends on |
|---|---|---|---|
| 0 | Foundation and First Deploy | 1–1.5 weeks | — |
| 1 | Authentication and Isolation | 1–1.5 weeks | 0 |
| 2 | Accounts and Categories | 3–5 days | 1 |
| 3 | Transactions | 1–1.5 weeks | 2 |
| 4 | Analytics | 1–1.5 weeks | 3 |
| 5 | Internal Dogfooding | 1 week (soak) | 4 |
| 6 | Statement Import | 2–3 weeks | 4 |
| 7 | Normalization and Duplicates | 1 week | 6 |
| 8 | Dogfood Statements | 1 week (soak) | 7 |
| 9 | Recurring Detection | 1 week | 8 |
| 10 | Budgets and Goals | 3–5 days | 4 |
| 11 | AI Categorization | 1–1.5 weeks | 9, D-03 |
| 12 | AI Assistant | 1–1.5 weeks | 11 |
| 13 | AI Insights | 3–5 days | 12 |
| 14 | Beta Readiness | 1.5–2 weeks | 13 |

**Total: ~19–22 weeks solo full-time.** Phase 5 and Phase 8 are calendar soak time more than build effort and can overlap with early work on the following phase where the roadmap allows it (Phase 5 findings should still land before Phase 6 design is final). Phase 10 has no dependency on Phase 6–9 and can run in parallel with Phases 6–9 if a second contributor is available.

## Phase 0 — Foundation and First Deploy

**Duration:** 1–1.5 weeks

**Tasks:** repository scaffold; Spring Boot backend running locally via Maven against file-based H2, no Docker (ADR-018); Flyway wired to boot (empty migration set for now); React/TypeScript skeleton with the API client layer stub; Docker Compose + PostgreSQL for staging/production only, per `deployment.md`; CI (lint, build, test — the PostgreSQL-dependent tests run against a real PostgreSQL service in CI, not H2); Caddy reverse proxy with automatic TLS; DNS to the ADR-017 droplet; nightly `pg_dump` job; `restore-drill.sh` committed.

**Status:** backend skeleton done — builds, boots, `GET /api/v1/health` responds, module package layout matches `lld.md`, error-contract handling in place. Remaining: React skeleton, CI, Docker Compose for prod, Caddy/TLS/DNS, backup job.

**Depends on:** D-05 (resolved: DigitalOcean, Bangalore, 4 GB — ADR-017).

**Exit gate:** a push to `main` reaches production automatically.

## Phase 1 — Authentication and Isolation

**Duration:** 1–1.5 weeks

**Tasks:** `users`, `sessions`, `password_reset_tokens` migrations; register/login/logout; cookie session per ADR-009; password-reset flow through Resend (ADR-016), including SPF/DKIM domain verification; `SET LOCAL app.current_user_id` transaction interceptor; RLS enabled and forced on every user-owned table so far; cross-user access test matrix; pooled-connection leakage test.

**Depends on:** Phase 0; D-04 (resolved: Resend — ADR-016).

**Exit gate:** the cross-user access matrix returns 404 for every resource type, and the pooled-connection leakage test passes.

## Phase 2 — Accounts and Categories

**Duration:** 3–5 days

**Tasks:** account CRUD and deactivate; seed the 19-row system category set from ADR-015; `categories` RLS exception policy for system rows; merchant table and basic normalization stub.

**Depends on:** Phase 1; D-02 (resolved: flat taxonomy — ADR-015).

## Phase 3 — Transactions

**Duration:** 1–1.5 weeks

**Tasks:** full ledger-side type enum; `transaction_splits` table and `transaction_category_allocations` view; manual entry with validation; `POST /transactions/transfer` creating linked pairs atomically; one-sided-transfer review flagging; idempotency key handling.

**Depends on:** Phase 2.

## Phase 4 — Analytics

**Duration:** 1–1.5 weeks

**Tasks:** monthly summary, category/merchant breakdown, trends, month-over-month comparison, currency scoping and exclusion reporting — all per `analytics-specification.md`; the six reconciliation invariants as unit tests; the September worked example as a fixture.

**Depends on:** Phase 3.

**Exit gate:** all six reconciliation invariants pass, and the worked example is a green test fixture.

## Phase 5 — Internal Dogfooding

**Duration:** ~1 week soak, low build effort

**Tasks:** none beyond bug fixes surfaced by real use. Track real personal spending using manual entry only, no statement import yet.

**Depends on:** Phase 4.

**Exit gate:** the app is genuinely easier to reach for than whatever it replaces. If not, fix the core loop before starting Phase 6 — statement import will not rescue a weak manual-entry loop.

## Phase 6 — Statement Import

**Duration:** 2–3 weeks

**Tasks:** upload and validation (MIME/signature/size/page-limit/encryption checks), secure object storage on DO Spaces (ADR-017), native text extraction with OCR fallback, format detection and three parsers for HDFC Bank, SBI and Axis Bank (ADR-014, `UNSUPPORTED_FILE` for anything else), staging table, confidence scoring, review UI, idempotent confirm endpoint.

**Depends on:** Phase 4; D-01 (resolved: HDFC, SBI, Axis — ADR-014).

## Phase 7 — Normalization and Duplicates

**Duration:** 1 week

**Tasks:** merchant normalization rules, duplicate scoring (exact reference, date+amount+merchant, similarity tiers), overlapping-statement-period handling, review overrides with audit metadata.

**Depends on:** Phase 6.

## Phase 8 — Dogfood Statements

**Duration:** ~1 week soak, moderate fix effort

**Tasks:** import real personal statements from the three supported banks; fix whatever the real layouts break. This is expected to surface parser edge cases no synthetic fixture caught.

**Depends on:** Phase 7.

## Phase 9 — Recurring Detection

**Duration:** 1 week

**Tasks:** pattern detection over confirmed transactions, frequency inference, `next_expected_date`, monthly/yearly estimate, subscription list view, dismiss action.

**Depends on:** Phase 8.

## Phase 10 — Budgets and Goals

**Duration:** 3–5 days

**Tasks:** budget and `budget_categories` CRUD, spend-vs-limit progress, savings goal CRUD and progress tracking.

**Depends on:** Phase 4 only — schedulable in parallel with Phases 6–9 if staffing allows.

## Phase 11 — AI Categorization

**Duration:** 1–1.5 weeks

**Tasks:** rules and merchant-history layer first (the model only sees genuinely ambiguous rows); `AIModelClient` egress chokepoint; structured-output schema validation against the ledger-side type set; confidence threshold and review-queue fallback; per-user daily token quota and global monthly cost ceiling; `AI_ENABLED` kill switch.

**Depends on:** Phase 9; D-03 (LLM provider and model — still open, see `open-decisions.md`). This phase cannot start until D-03 is decided.

## Phase 12 — AI Assistant

**Duration:** 1–1.5 weeks

**Tasks:** finance tool registry (`monthly_summary`, `category_spending`, `top_merchants`, `spending_trend`, `recurring_expenses`, `budget_status`, `goal_status`), session-derived user identity wired through every tool call, structural separation of data vs. instructions in the prompt, chat UX, golden dataset including injection-attempt rows.

**Depends on:** Phase 11.

**Exit gate:** grounding tests pass — every monetary claim in an AI answer maps to a value returned by a tool in that exchange.

## Phase 13 — AI Insights

**Duration:** 3–5 days

**Tasks:** monthly-summary and unusual-spending explanation generation over verified metrics only; `ai_insights` rows record `model_name` and `prompt_version` for attribution.

**Depends on:** Phase 12.

## Phase 14 — Beta Readiness

**Duration:** 1.5–2 weeks

**Tasks:** rate limiting on auth, upload and AI endpoints; full metrics/alerting per `observability-monitoring.md`; privacy policy and DPDP-compliant deletion path reviewed by someone qualified; export endpoint; incident response plan; signed-off restore drill; uptime monitoring.

**Depends on:** Phase 13.

**Exit gate:** the internal-to-beta checklist in `development-roadmap.md` is complete.

## Remaining Open Decision

D-03 (LLM provider and model) is the only item still blocking a phase — it gates Phase 11. Decide it before Phase 9 finishes so Phase 11 is not idle waiting on it.
