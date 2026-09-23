# Edge Cases

## Transactions
Zero and invalid amounts, future dates, refunds crossing period boundaries, one-sided transfers, unknown merchants, missing categories, mixed currencies, and splits that do not sum to the parent amount.

## Cards
Purchases, bill payments as paired rows, refunds, annual fees, interest charged, and cash advances.

## Statements
Empty, scanned, password-protected and corrupt PDFs. Multiple accounts in one file. Duplicate uploads detected by file hash. Overlapping periods. Reversed rows. Missing dates. Multi-page tables. Repeated confirm calls.

## Analytics
- No data in the period: return zeros, not nulls, with an empty breakdown.
- Zero income: `savings_rate` is `null`, never `0`. The UI renders an em dash.
- Negative savings: displayed as-is, never clamped.
- A refund later than its purchase: the category total may go negative, and is displayed as-is.
- Partial statement coverage: figures reflect confirmed data only and the UI says so.
- Mixed currencies: excluded rows are reported in `meta` and surfaced, never silently dropped.
- Unknown-type rows: excluded from figures and surfaced as a review prompt.

## AI
Provider outage, rate limits, quota exhaustion, invalid structured output, tool timeout, ambiguous period or category, unsupported requests, and ungrounded numbers.

## Security
Cross-tenant access attempts, forged account identifiers, unauthorized file access, prompt injection through transaction descriptions, session fixation and reuse, and sensitive values reaching logs.
