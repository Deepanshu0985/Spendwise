# User Stories & Acceptance Criteria

## US-001 Manual Expense
Given an authenticated user, when amount, date, account and category are valid, create a confirmed transaction and update analytics.

## US-002 Income
User records income; the period's income and savings update.

## US-003 Statement Upload
User uploads a supported PDF; it is validated, stored and processed into `READY_FOR_REVIEW`.

## US-004 Review Import
User can edit, select and deselect parsed rows. Only selected approved rows become canonical transactions. Confirming twice imports nothing twice.

## US-005 Duplicate Detection
Likely duplicates are flagged with evidence and confidence. Medium-confidence cases remain reviewable.

## US-006 Dashboard
For a selected period, show verified income, expenses, savings, savings rate, categories, merchants and trends, in a single currency, with any excluded currencies stated.

## US-007 Budget
User creates a budget and category limits; progress updates after confirmed transactions.

## US-008 Goal
User creates a savings target and sees current and target progress.

## US-009 AI Categorization
A candidate receives a structured suggestion with confidence. Rules and merchant history are applied before the model. Low-confidence results require review.

## US-010 AI Assistant
A financial question causes an approved finance tool to return deterministic data, which AI explains without altering the values.

## US-011 Monthly Insight
Backend supplies verified metrics; AI creates an explanation tied to that period.

## US-012 Export
User can export only their authorized data.

## US-013 Failure Recovery
AI outages do not break manual tracking or analytics. PDF failures produce an actionable `FAILED` status.

## US-014 Transfer Between Own Accounts
User records a transfer; both halves are created with a shared group identifier, and neither income nor expenses change.

## US-015 Session Control
User can see where they are signed in and end all sessions at once. Ending sessions takes effect immediately.

## US-016 Cross-User Isolation
A request for another user's resource returns 404, disclosing nothing about whether that resource exists.
