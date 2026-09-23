# API Error Contract

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "One or more fields are invalid.",
    "details": [{"field": "amount", "reason": "must be greater than zero"}],
    "requestId": "uuid"
  }
}
```

## Codes
- `VALIDATION_ERROR` 400
- `UNAUTHORIZED` 401
- `FORBIDDEN` 403
- `NOT_FOUND` 404
- `CONFLICT` 409
- `FILE_TOO_LARGE` 413
- `UNSUPPORTED_FILE` 415
- `STATEMENT_PROCESSING_FAILED` 422
- `RATE_LIMITED` 429
- `AI_QUOTA_EXCEEDED` 429
- `AI_UNAVAILABLE` 503
- `INTERNAL_ERROR` 500

## Ownership Failures Return 404

A request for a resource belonging to another user returns `NOT_FOUND`, never `FORBIDDEN`. Returning 403 would confirm that the identifier exists, which leaks information across tenants. Row-Level Security produces this behaviour naturally, since the row is invisible rather than protected.

## Quota Failures

`AI_QUOTA_EXCEEDED` is returned when a per-user or global AI cost cap is reached. The response must state which limit applied and when it resets. Core tracking and analytics remain fully available.

Never expose stack traces, SQL, secrets, tokens or provider credentials.
