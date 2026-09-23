# Analytics Specification

This document defines every number the product displays. All figures are computed deterministically by the ANALYTICS module. AI never produces, adjusts or rounds them.

## Inclusion Rules

A transaction enters analytics only when all of the following hold:

- `status = CONFIRMED`. `PENDING`, `IGNORED` and `DELETED` are excluded.
- `transaction_type != UNKNOWN`. Unresolved rows are excluded and surfaced separately as a review prompt.
- `currency` matches the requested analytics currency (see Currency Scoping).

Staging rows in `statement_transactions` never appear in analytics under any circumstance.

## Transaction Types and Ledger Sides

| Type | Side | In expenses | In income | Paired |
|---|---|---|---|---|
| `EXPENSE` | debit | yes | no | no |
| `INCOME` | credit | no | yes | no |
| `REFUND` | credit | reduces | no | no |
| `FEE_CHARGED` | debit | yes | no | no |
| `INTEREST_CHARGED` | debit | yes | no | no |
| `INTEREST_EARNED` | credit | no | yes | no |
| `TRANSFER_OUT` | debit | no | no | yes |
| `TRANSFER_IN` | credit | no | no | yes |
| `CARD_PAYMENT_OUT` | debit | no | no | yes |
| `CARD_PAYMENT_IN` | credit | no | no | yes |
| `CASH_WITHDRAWAL` | debit | no | no | no |
| `UNKNOWN` | — | no | no | no |

Paired types share a `transfer_group_id`. Both rows are created together when both accounts are tracked. When only one side is known — a transfer to an external account — the row is created one-sided and flagged for review rather than silently dropped.

`CASH_WITHDRAWAL` is a move to an untracked cash pool, never itself an expense. When the user has a `CASH` account, a `TRANSFER_OUT`/`TRANSFER_IN` pair is preferred and cash spending is recorded as `EXPENSE` against that account.

## Formulas

```text
expenses     = SUM(amount where in_expenses) - SUM(amount where type = REFUND)
income       = SUM(amount where in_income)
savings      = income - expenses
savings_rate = income > 0 ? (savings / income * 100) : null
```

`savings_rate` is `null`, never `0`, when income is zero: a user with no recorded income has an undefined savings rate, not a zero one. The API returns `null` and the UI renders an em dash.

`savings` may be negative. A category total may be negative when a refund lands in a period later than its original purchase. Both are displayed as-is and never clamped.

## Period Semantics

`transaction_date` is a calendar date (`LocalDate`), not an instant. No timezone conversion is ever applied to it. A month is the inclusive range from its first to its last day in the user's own calendar. `users.timezone` governs display timestamps and scheduling only, never transaction bucketing.

## Currency Scoping

Every analytics request resolves to exactly one currency, defaulting to `users.default_currency`. Transactions in other currencies are excluded from all aggregates and the response reports what was left out.

```json
{
  "data": { "currency": "INR", "expenses": 42150.0000, "income": 80000.0000 },
  "meta": { "excludedCurrencies": ["USD"], "excludedTransactionCount": 2 }
}
```

Silent exclusion is forbidden. The UI must surface `excludedCurrencies` wherever it appears. Cross-currency conversion is out of scope for V1.

## Category Aggregation

All category figures read the `transaction_category_allocations` view, never `transactions.category_id` directly. Transactions with no category fall into a reserved `Uncategorized` bucket rather than being dropped. Merchant breakdowns place unmatched rows in an `Unknown merchant` bucket on the same principle.

## Precision

Amounts are `NUMERIC(19,4)` end to end. Intermediate results are never rounded. Rounding occurs only at the display layer, and display rounding never feeds back into a calculation.

## Reconciliation Invariants

Each of these is a unit test. A failure blocks release.

1. `SUM(category_breakdown) == expenses` for the same period, currency and filters.
2. `SUM(merchant_breakdown) == expenses`.
3. `SUM(trend_series) == period_total` for every grouping.
4. `SUM(allocations for a transaction) == transaction.amount`.
5. `income - expenses == savings`.
6. For every `transfer_group_id`, the debit and credit rows are equal in magnitude and the group contributes zero to both income and expenses.

Invariant 6 is what proves the double-counting rule actually holds.

## Worked Example

One credit card and one bank account, September:

| Date | Description | Type | Account | Amount |
|---|---|---|---|---|
| Sep 3 | Swiggy | `EXPENSE` | Card | 500 |
| Sep 8 | Groceries | `EXPENSE` | Card | 2,000 |
| Sep 20 | Card bill | `CARD_PAYMENT_OUT` | Bank | 2,500 |
| Sep 20 | Card bill | `CARD_PAYMENT_IN` | Card | 2,500 |
| Sep 25 | Swiggy refund | `REFUND` | Card | 500 |
| Sep 30 | Salary | `INCOME` | Bank | 80,000 |

`expenses = (500 + 2000) - 500 = 2,000`. The card bill payment contributes nothing.
`income = 80,000`. `savings = 78,000`. `savings_rate = 97.5`.

This example is a required test fixture. It exercises every rule in this document at once.
