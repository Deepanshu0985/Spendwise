# Architecture Decision Records

## ADR-001 Modular Monolith
V1 uses a modular monolith to minimize operational complexity while retaining strong module boundaries.

## ADR-002 PostgreSQL
PostgreSQL is the primary store because financial records need relational integrity, transactions, indexing and flexible JSONB support.

## ADR-003 Canonical Transactions
All manual and imported financial events converge into one canonical transaction model.

## ADR-004 Statement Staging
PDF rows never bypass staging/review because parsing may be uncertain and duplicate-prone.

## ADR-005 Deterministic Analytics
Backend services calculate financial figures; AI explains verified results.

## ADR-006 Tool-Based AI
AI can call only allowlisted finance tools. It cannot execute arbitrary SQL.

## ADR-007 Async Heavy Processing
Large PDFs, OCR, recurring detection and insight generation run asynchronously.

## ADR-008 AI Optional
The core product must continue functioning when the AI provider is unavailable.

## ADR-009 Cookie Sessions
**Decision.** Authentication uses server-side sessions carried in an `httpOnly; Secure; SameSite=Lax` cookie, not JWTs.

**Context.** V1 is browser-only and runs as a single instance, so statelessness buys nothing. Revocation matters: users of a finance app expect logout to be real and expect "log out everywhere" to exist.

**Consequences.** A `sessions` table stores the hashed token, so a database leak does not yield live sessions. Logout deletes the row. CSRF tokens are required on state-changing requests as defense in depth alongside `SameSite=Lax`. A `password_reset_tokens` table supports account recovery. `JWT_SECRET` is removed from configuration. If a native mobile client is built later, a token issuer is added then — that is a reversible decision.

## ADR-010 Row-Level Security for Tenant Isolation
**Decision.** Tenant isolation is enforced by PostgreSQL Row-Level Security, with repository-level predicates layered on top for query ergonomics.

**Context.** Isolation enforced only by application discipline fails eventually — one native query or one forgotten predicate leaks another user's financial data. That failure is unrecoverable for a product built on trust. Retrofitting database-level enforcement after the query surface has grown is impractical.

**Consequences.** Every user-owned table carries a `user_id` column, denormalized where the relationship is indirect, and enables both `ROW LEVEL SECURITY` and `FORCE ROW LEVEL SECURITY` — without `FORCE`, the owning role bypasses every policy. The current user is set with `SET LOCAL app.current_user_id` inside the transaction; plain `SET` persists on pooled connections and would serve one user's data to the next request. `categories` carries an exception policy for shared system rows. Migrations run under a role with `BYPASSRLS`. A cross-user access test matrix and a pooled-connection leakage test are CI release gates.

## ADR-011 Single-Currency Aggregation
**Decision.** Transactions store their own currency. Analytics resolve to exactly one currency per request and never sum across currencies. FX conversion is out of scope for V1.

**Context.** Indian statements present foreign transactions already converted to INR, so mixed currency is rare but real for users holding a foreign account. Silently summing across currencies produces figures that are wrong without appearing wrong.

**Consequences.** Analytics endpoints accept a `currency` parameter defaulting to `users.default_currency`, and report excluded currencies and counts in response `meta`. The UI must surface those exclusions. Budgets and goals must match the currency of what they scope.

## ADR-012 Explicit Ledger-Side Transaction Types
**Decision.** Transaction types name their ledger side explicitly. `INTEREST` splits into `INTEREST_EARNED` and `INTEREST_CHARGED`, `FEE` becomes `FEE_CHARGED`, and `TRANSFER` and `CARD_PAYMENT` each split into `_OUT` and `_IN` halves linked by a shared `transfer_group_id`.

**Context.** A single `INTEREST` type is ambiguous: interest earned on a savings account is income, interest charged on a credit card is an expense. The same ambiguity applies to fees. Transfers and card payments are two-sided events that a single row cannot represent without the account balance drifting.

**Consequences.** Type alone determines the ledger side, so invalid states are unrepresentable and no check constraint is needed. The type-to-ledger-side mapping in `analytics-specification.md` becomes the authoritative analytics contract. Paired rows are created together when both accounts are tracked, and flagged for review when only one side is known. `CASH_WITHDRAWAL` remains a one-sided move to an untracked cash pool; when the user has a `CASH` account, a transfer pair is preferred.

## ADR-013 Split-Ready Schema, Deferred UI
**Decision.** A `transaction_splits` table ships in the initial migration sequence. All category aggregation reads the `transaction_category_allocations` view. The split-editing UI is deferred past MVP.

**Context.** Mixed-category purchases are common and users of a "where did my money go" product will ask for splits. Adding the table later is cheap only if every category aggregation is centralized; making that structural now removes the risk entirely.

**Consequences.** `transactions.category_id` remains for V1 and represents the single-category case. Analytics never read it directly. Enabling splits later is frontend work plus removing the view's fallback branch, with no analytics rewrite. The invariant that splits sum to the transaction amount is enforced in the service layer with a test rather than a database trigger.

## ADR-014 MVP Statement Formats
**Decision.** V1 statement import supports HDFC Bank, State Bank of India (SBI) and Axis Bank PDF statement formats only.

**Context.** Phase 8 of the roadmap dogfoods the parser against real personal statements; scoping to the banks the builder actually holds accounts with is what makes that phase meaningful rather than speculative. Every additional bank format adds real parsing effort for a layout that cannot be tested against real data until it is added.

**Consequences.** The parser registry (`StatementParser` implementations, see `lld.md`) ships three format handlers for V1. Additional banks are added post-MVP as format detection (`05-statement-processing/pdf-processing.md`) proves out. An unsupported format fails with an actionable `UNSUPPORTED_FILE` error rather than a silent misparse.

## ADR-015 Default Category Taxonomy
**Decision.** V1 ships a flat, non-hierarchical set of system categories: Food & Dining, Groceries, Transportation, Shopping, Bills & Utilities, Rent/Housing, Entertainment, Health & Fitness, Travel, Education, Personal Care, Subscriptions, Insurance, Fees & Charges, Salary, Interest, Refunds, Other Income, and a reserved Uncategorized bucket.

**Context.** `categories.parent_id` exists in the schema for future hierarchy, but a taxonomy this size does not need nesting yet, and shipping a hierarchy before real usage data exists risks modeling categories no one uses. Users can add custom categories alongside the system set from V1.

**Consequences.** These rows are seeded as `is_system = true`, visible to every tenant through the RLS exception policy in `database-design.md`. `Fees & Charges`, `Interest` and `Refunds` map to `FEE_CHARGED`/`INTEREST_CHARGED`/`INTEREST_EARNED`/`REFUND` transaction types respectively, so the AI categorizer and manual entry share one vocabulary. Hierarchy can be introduced later without a migration, since `parent_id` already exists.

## ADR-016 Transactional Email Provider
**Decision.** Resend is the email provider for password reset and account verification.

**Context.** V1 email volume is low — auth flows only — and the build is solo. Resend's API and free tier fit that shape better than AWS SES's sandbox-mode setup overhead or Postmark's pricing.

**Consequences.** `MAIL_PROVIDER=resend` and `MAIL_API_KEY` are configured per `08-devops/environments.md`. Domain verification (SPF/DKIM) for the sending domain is a Phase 1 prerequisite, since password reset sits on the critical path for account recovery. Switching providers later is isolated to the mail-sending port and does not touch application logic.

## ADR-017 Hosting Provider and Region
**Decision.** V1 production runs on a single DigitalOcean droplet in the Bangalore (BLR1) region, sized per the 4 GB plan in `08-devops/deployment.md`.

**Context.** DigitalOcean offers an India region, which matters for latency to an India-based user base, plus Docker-friendly droplets and predictable pricing that suit a budget-conscious solo deployment.

**Consequences.** Statement object storage uses DigitalOcean Spaces (S3-compatible) in the same region, so `STORAGE_ENDPOINT` targets the BLR1 Spaces endpoint and there is no cross-region egress. Moving to the later multi-node shape in `deployment.md` stays on the same provider, so no infrastructure-tooling migration is needed when that step is taken.

## ADR-018 Local Development Without Docker
**Decision.** Local backend development runs directly via Maven against a file-based H2 database, with Spring configuration in `application.properties` (no YAML, no XML bean configuration). Docker Compose and PostgreSQL remain the target for staging and production, unchanged from ADR-002 and `08-devops/deployment.md`.

**Context.** A solo build benefits from the fastest possible local edit-run loop; installing and running Docker Desktop plus a PostgreSQL container adds friction with no corresponding benefit before the schema or the query surface exists. Flyway still owns the schema in dev, so what runs against H2 is the same migration history that later runs against PostgreSQL, keeping the two paths from silently diverging.

**Consequences.** Spring profiles separate the two paths: `dev` (default) targets file-based H2, `test` targets in-memory H2, `prod` targets PostgreSQL via environment variables. PostgreSQL-specific SQL (if any migration ever needs it) will not be caught by local H2 runs — CI must run the migration and test suite against a real PostgreSQL instance before merge, not only against H2. Row-Level Security (ADR-010) is a PostgreSQL feature with no H2 equivalent: the cross-tenant isolation test matrix and the pooled-connection leakage test, both release gates in `testing-strategy.md`, only mean anything against PostgreSQL and must run there, never against the local H2 database.

**Update (Phase 1).** The `dev` profile now targets a Neon Postgres branch instead of file-based H2 — see `DECISIONS.md` ("Neon Postgres replaces H2 for local development"). This was foreseeable from this ADR's own consequences above: Phase 1 introduces Row-Level Security, which H2 cannot exercise at all, and Neon removes the Docker/local-install friction this ADR was written to avoid while still being real PostgreSQL. "No Docker for local development" itself is unchanged; only the "H2" part of the original decision is superseded. `test` still uses in-memory H2 for fast unit tests that don't depend on Postgres-specific behavior.
