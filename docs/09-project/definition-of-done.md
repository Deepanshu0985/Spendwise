# Definition of Done

## Requirements
Acceptance criteria satisfied, edge cases considered, user-facing behaviour documented.

## Backend
Validation, authorization, error contract, migrations and safe logging complete. New user-owned tables carry `user_id` and have Row-Level Security enabled and forced, with a policy and an isolation test.

## Frontend
Loading, empty, success and error states. Responsive behaviour. Accessibility basics. No financial calculation duplicated in the UI; display rounding never feeds back into a value.

## Testing
Unit tests for business logic, integration and API tests, regression tests, and E2E coverage for critical journeys. Any change touching a financial figure re-runs the reconciliation invariants.

## AI
Structured output validated, tools permissioned, user identity session-derived, financial figures grounded, evaluation cases added, injection case added, provider failure handled, quota enforced.

## Statements
Raw input preserved, staging and review boundary intact, duplicate detection tested, low-confidence rows reviewable, confirm idempotent.

## Operations
Metrics and logging for important failures, environment-driven configuration, additive-only migrations, and a rollback path that works against the migrated schema.

## Documentation
Any decision made during the work is recorded as an ADR, and its row in `09-project/open-decisions.md` is updated in the same commit.

## Release
CI passes, security checks pass, all release gates in `07-quality/testing-strategy.md` are green, staging smoke tests pass where staging exists, documentation is current.
