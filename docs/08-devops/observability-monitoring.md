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

## What is implemented (free-tier setup)

- **Request metrics in memory** (`RequestMetrics`, `RequestMetricsFilter`): requests, server errors, rate-limit refusals, failed sign-ins and a latency histogram per minute, never keyed by user. Every five minutes with traffic, one `ops-metrics` line goes to the log; there is no dashboard.
- **Alerts** (`OpsMonitor`, `AlertService`): a sustained run of server errors (at least 5 and 20% of the last five minutes' requests), a burst of rate-limit refusals (100 in five minutes), and AI use reaching 50%, 80% and 100% of each monthly cap. One alert per condition per hour; AI levels once per month. Delivered to `ALERT_WEBHOOK_URL`: with no value they only appear in the log. ntfy.sh needs no account (`https://ntfy.sh/<a-long-random-topic>`, subscribe in the phone app); for Slack or Discord set `ALERT_WEBHOOK_FORMAT` to `slack` or `discord`. Alert text never contains personal or financial data.
- **Health:** `GET /api/v1/health` (liveness, never touches the database) and `GET /api/v1/health/ready` (checks the database; 503 when it is down). AI is in neither.
- **Not implemented, deliberately:** statement-processing failure rates, per-user AI cost views and tracing. At this scale the log lines and the database tables (`ai_usage_daily`, `ai_usage_monthly`) answer those questions on demand.

## Uptime monitoring

`.github/workflows/uptime.yml` does this with no account: every five minutes it calls the production liveness URL, retries three times over about a minute (a cold start is not an outage), and on failure GitHub emails the owner and the `ALERT_WEBHOOK_URL` secret, if set, gets a push message. It runs from `main` once merged. A free external monitor (UptimeRobot or similar) is an alternative on the same **liveness** URL, `https://<backend>/api/v1/health`, every 5 minutes, alerting to email or the same ntfy topic. Two cautions for the free tiers:

- Do **not** point it at `/health/ready`: that queries the database and would keep a scale-to-zero Neon database awake around the clock and use up its free compute hours.
- A 5-minute ping also keeps the free Render service from sleeping, which uses its monthly instance hours (750 hours is enough for exactly one always-on service). Monitor only the production backend, not staging, unless you accept staging sleeping.

Backups and the restore drill are in `backup-and-restore.md`; record each drill there.
