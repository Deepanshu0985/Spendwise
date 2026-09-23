# Observability & Monitoring

## Metrics
- API request count, latency and error rate.
- Authentication failures and rate-limit rejections.
- Statement processing duration and failure rate.
- OCR invocations and duration.
- Import duplicate and low-confidence rates.
- Database latency and errors.
- AI latency, tokens, cost and provider failures, per user and in aggregate.
- AI quota rejections and kill-switch state.
- Background job success and failure.
- Backup success and last successful restore drill.

## Cost Controls
Tracking cost is not the same as capping it, and AI is the only line item that scales with usage rather than with traffic. Two limits are enforced before a provider call is dispatched:

- A per-user daily token quota, which returns `AI_QUOTA_EXCEEDED` when reached.
- A global monthly cost ceiling, which disables AI features and leaves all deterministic functionality intact.

`AI_ENABLED` is a kill switch that takes effect without a deploy. Alert on reaching 50% and 80% of the global ceiling, not only on breaching it.

## Logs
Structured logs with request identifiers. Never log passwords, tokens, session identifiers, API keys or complete financial documents.

## Tracing
Trace request through service to repository and external processing where practical.

## Alerts
Sustained API errors, database failures, statement-processing failure spikes, background job failures, unusual AI provider errors, AI spend thresholds, and any failed backup or restore drill.

## Health
Separate liveness and readiness checks, with dependency health reported on readiness. AI provider health never affects readiness, because the product is required to function without it.
