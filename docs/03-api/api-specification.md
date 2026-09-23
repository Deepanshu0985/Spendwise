# API Specification

Base path: `/api/v1`

## Auth
`POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/sessions`, `DELETE /auth/sessions`, `POST /auth/password-reset`, `POST /auth/password-reset/confirm`, `GET /users/me`

See `authentication-api.md`.

## Accounts
`GET/POST /accounts`, `GET/PUT/DELETE /accounts/{id}`

## Transactions
`GET/POST /transactions`, `GET/PUT/DELETE /transactions/{id}`

`DELETE` is a soft delete: it sets `status = DELETED` and retains the row. Financial records are never physically removed.

Filters: `from`, `to`, `accountId`, `categoryId`, `merchantId`, `type`, `status`, `currency`, `page`, `size`, `sort`.

`size` is capped at 200. Requests above the cap are clamped and the applied value is reported in `meta`.

Example request:
```json
{
  "accountId": "uuid",
  "categoryId": "uuid",
  "transactionDate": "2026-09-18",
  "amount": 350.0000,
  "currency": "INR",
  "description": "Lunch",
  "transactionType": "EXPENSE"
}
```

### Transfers and Card Payments
`POST /transactions/transfer`
```json
{
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "transactionDate": "2026-09-20",
  "amount": 2500.0000,
  "currency": "INR",
  "kind": "TRANSFER"
}
```

Creates both halves atomically with a shared `transfer_group_id`. `kind` is `TRANSFER` or `CARD_PAYMENT`. When only one account is tracked, the created row is one-sided and flagged for review.

## Categories
`GET/POST /categories`, `PUT/DELETE /categories/{id}`

## Merchants
`GET/POST /merchants`, `PUT /merchants/{id}`

## Analytics
`GET /analytics/monthly`
`GET /analytics/categories`
`GET /analytics/merchants`
`GET /analytics/trends`

All analytics endpoints accept `currency`, defaulting to the user's default currency, and a period bounded to a maximum of 36 months. Figures follow `analytics-specification.md` exactly.

## Statements
`POST /statements/upload`
`GET /statements`
`GET /statements/{id}`
`GET /statements/{id}/transactions`
`PUT /statements/{id}/transactions/{stagingId}`
`POST /statements/{id}/confirm`
`POST /statements/{id}/retry`

`confirm` is idempotent: it is valid only from `READY_FOR_REVIEW`, and a repeat call against an already-`IMPORTED` statement returns the original result rather than importing twice.

## Budgets
`GET/POST /budgets`, `GET/PUT/DELETE /budgets/{id}`

## Goals
`GET/POST /goals`, `PUT/DELETE /goals/{id}`

## Recurring
`GET /recurring-expenses`, `PUT /recurring-expenses/{id}`, `POST /recurring-expenses/{id}/dismiss`

## AI
`POST /ai/chat`, `POST /ai/categorize`, `POST /ai/insights/monthly`

Subject to per-user and global quotas; `AI_QUOTA_EXCEEDED` is returned when a cap is reached.

## Export
`POST /export`, `GET /export/{id}`

## Response Envelope
```json
{"data": {}, "meta": {}}
```

`meta` carries pagination, applied page size, and for analytics the `excludedCurrencies` and `excludedTransactionCount` described in `analytics-specification.md`.

## Authorization
Every resource access is scoped to the authenticated user in the application layer and enforced independently by Row-Level Security. Identifiers supplied by the client are never trusted as proof of ownership. Cross-tenant requests return 404.

## Idempotency
`POST /transactions`, `POST /transactions/transfer` and `POST /statements/{id}/confirm` accept an `Idempotency-Key` header. A repeated key returns the original response rather than creating a second record.
