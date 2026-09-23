# Authentication API

Authentication uses server-side sessions carried in a cookie. See ADR-009.

## Register
`POST /api/v1/auth/register`
```json
{"email":"user@example.com","password":"strong-password","fullName":"Example User"}
```

## Login
`POST /api/v1/auth/login`
```json
{"email":"user@example.com","password":"strong-password"}
```

Creates a `sessions` row and returns the session cookie.

## Logout
`POST /api/v1/auth/logout`

Deletes the current session row. Logout is immediate and complete.

## List Sessions
`GET /api/v1/auth/sessions`

Returns active sessions with `createdAt`, `lastSeenAt` and `userAgent` so the user can see where they are signed in.

## Revoke All Sessions
`DELETE /api/v1/auth/sessions`

Ends every session for the user. Expected behaviour for a finance application.

## Request Password Reset
`POST /api/v1/auth/password-reset`
```json
{"email":"user@example.com"}
```

Always returns 202 regardless of whether the address exists, so the endpoint does not disclose which emails are registered.

## Complete Password Reset
`POST /api/v1/auth/password-reset/confirm`
```json
{"token":"opaque-token","password":"new-strong-password"}
```

Consumes the token and revokes all existing sessions.

## Current User
`GET /api/v1/users/me`

## Cookie Properties
`httpOnly`, `Secure`, `SameSite=Lax`, `Path=/`.

## Session Lifecycle
- Idle timeout governed by `SESSION_IDLE_TIMEOUT`, refreshed on use via `last_seen_at`.
- Absolute expiry governed by `SESSION_TTL`, never extended.
- The token is stored hashed; the plaintext exists only in the cookie.
- Expired and revoked rows are purged by a scheduled job.

## Requirements
- Adaptive password hashing.
- Rate-limited login, registration and password-reset endpoints.
- CSRF token required on state-changing requests, alongside `SameSite=Lax`.
- Never log credentials, tokens or session identifiers.
- Password policy validated server-side.
- Authorization is enforced server-side for every resource, backed by Row-Level Security (ADR-010).
