# Development Roadmap

Deterministic finance foundations come before AI. Deployment and tenant isolation come before features.

## Phase 0 — Foundation and First Deploy
Repository, Docker Compose, Spring Boot skeleton, React/TypeScript skeleton, PostgreSQL, Flyway, CI. Deploy a hello-world to the VPS through the full pipeline. Caddy, TLS, domain. Nightly backup and a committed restore script.

**Gate:** a push to the main branch reaches production automatically.

## Phase 1 — Authentication and Isolation
Cookie sessions, `sessions` table, password reset, registration, profile. Row-Level Security enabled and forced on every user-owned table, with the `SET LOCAL` transaction interceptor.

**Gate:** the cross-user access matrix returns 404 for every resource type, and the pooled-connection leakage test passes.

## Phase 2 — Accounts and Categories
Account CRUD and deactivation. Default category taxonomy (D-02). System categories with their RLS exception policy. Merchants.

## Phase 3 — Transactions
Full ledger-side type enum, `transaction_splits` table, `transaction_category_allocations` view, manual entry, validation, transfer pairing.

## Phase 4 — Analytics
Monthly summary, category and merchant breakdown, trends, month-over-month comparison — all per `analytics-specification.md`.

**Gate:** all six reconciliation invariants pass, and the worked example is a green test fixture.

## Phase 5 — Internal Dogfooding
Track real personal spending using manual entry only. No statement import yet.

**Gate:** the app is genuinely easier to reach for than whatever it replaces. If it is not, the core loop needs work and statement import will not rescue it.

## Phase 6 — Statement Import
Upload, validation, secure storage, native text extraction, OCR fallback, format detection, parsing, staging, review, confirmation. The confirm endpoint is idempotent.

## Phase 7 — Normalization and Duplicates
Merchant normalization, duplicate scoring, overlapping-period handling, review overrides with audit metadata.

## Phase 8 — Dogfood Statements
Import real personal statements from real banks. This is where parser reality lands, and it generates better fixtures than speculative format work.

## Phase 9 — Recurring Detection
Pattern detection, frequency, next expected date, subscription view.

## Phase 10 — Budgets and Goals
Budget definitions, category limits, progress and pacing, savings goals.

## Phase 11 — AI Categorization
Rules and merchant history first; the model sees only genuinely ambiguous rows. Structured output validation, confidence, review queue. Cost caps, per-user quotas and the kill switch ship **with** this phase, not after it.

## Phase 12 — AI Assistant
Tool registry, session-derived user identity, injection-resistant prompt structure, chat UX.

**Gate:** grounding tests pass — every monetary claim maps to tool output.

## Phase 13 — AI Insights
Monthly summaries and unusual-spending explanations over verified metrics.

## Phase 14 — Beta Readiness
Rate limiting, quota enforcement, observability and alerting, privacy policy, export and deletion, breach response plan, restore drill signed off, uptime monitoring.

**Gate:** the internal-to-beta checklist is complete.

## Ordering Rationale

- Deployment moved to Phase 0 so that deploying is routine before it carries real data. A first deploy at the end of the project is a large, avoidable risk.
- Isolation moved to Phase 1 because retrofitting it across a finished query surface is the failure ADR-010 exists to prevent.
- Two dogfooding phases are explicit rather than assumed, because they are the real first stage of this product.
- AI cost controls ship alongside the AI feature they meter.
