# Functional Requirements

## Authentication
- Register, login, logout and current-user profile.
- Password reset by email, single-use and time-limited.
- Session listing and revoke-all.
- Passwords are hashed with an adaptive algorithm.
- Every resource is scoped to the authenticated user and independently enforced by Row-Level Security.

## Accounts
- CRUD and deactivate accounts.
- Types: `BANK`, `CREDIT_CARD`, `CASH`, `WALLET`, `OTHER`.
- Store currency and optional institution and last-four metadata.

## Transactions
- Create, read, update and soft-delete according to status. Records are never physically removed.
- Types: `EXPENSE`, `INCOME`, `REFUND`, `FEE_CHARGED`, `INTEREST_CHARGED`, `INTEREST_EARNED`, `TRANSFER_OUT`, `TRANSFER_IN`, `CARD_PAYMENT_OUT`, `CARD_PAYMENT_IN`, `CASH_WITHDRAWAL`, `UNKNOWN`.
- Transfers and card payments are created as linked pairs sharing a `transfer_group_id`.
- Store fixed-precision amount, calendar date, account, merchant, category, description and source.
- Allocate a transaction across multiple categories (schema present from V1; editing UI deferred).
- Only confirmed transactions drive analytics.

## Categories and Merchants
- System and custom categories.
- Parent and child categories.
- Merchant normalization and association.

## Statement Import
- Upload and validate PDFs.
- Native text extraction with OCR fallback.
- Format detection and parsing.
- Staging, confidence scoring and duplicate detection.
- Review, edit and select rows before confirmation.
- Import approved rows only; confirmation is idempotent.
- Retry and actionable failure reporting.

## Analytics
- Income, expenses, savings and savings rate, per `analytics-specification.md`.
- Category and merchant breakdown.
- Daily and monthly trends.
- Month-over-month comparison.
- Exclude unconfirmed and unknown-type transactions.
- Resolve to a single currency per request and report exclusions.

## Recurring, Budgets and Goals
- Detect recurring patterns and expected dates.
- Create budgets and category limits.
- Track savings goals and progress.

## AI
- Suggest transaction category, type and merchant, with rules and merchant history applied before the model.
- Answer financial questions through allowlisted tools only.
- Generate monthly insights over verified metrics.
- Validate structured output against a schema.
- Never invent financial figures.
- Enforce per-user and global cost caps, with a kill switch that disables AI without a deploy.

## Privacy
- Export data.
- Deletion request.
- Minimize data sent to external AI.
- Never log secrets, tokens or unnecessary financial detail.
