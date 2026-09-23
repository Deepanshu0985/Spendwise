# AI Tool Specification

All tools are allowlisted backend functions. The model selects among them; it never reaches the database.

## monthly_summary
Input: period start and end. Output: income, expenses, savings, savings rate, currency.

## category_spending
Input: category and period. Output: verified amount and transaction count.

## top_merchants
Input: period and limit. Output: verified merchant totals.

## spending_trend
Input: period and grouping. Output: verified time series.

## recurring_expenses
Input: `activeOnly`. Output: detected recurring records.

## budget_status
Input: budget identifier. Output: limit, spent, remaining, percentage.

## goal_status
Input: goal identifier. Output: target, current, remaining, target date.

## Tool Rules
- **User identity comes from the authenticated session, never from a tool argument.** No tool accepts a user identifier as a parameter. This is the primary control against prompt injection reaching another tenant's data.
- All arguments are schema-validated and range-checked before execution.
- Tools are read-only.
- Tools return typed deterministic values produced by the analytics services, computed per `analytics-specification.md`.
- No arbitrary SQL, ever.
- Period arguments are bounded; an unbounded range is rejected rather than clamped silently.
- Every invocation is counted against the caller's AI quota.
- Tool execution is logged with the tool name and argument shape, never with raw financial values.
