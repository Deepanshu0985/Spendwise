# Transaction Normalization

## Input
Date, amount, debit/credit marker, raw description, reference, balance and source row position.

## Canonical Example
```json
{
  "transactionDate": "2026-09-18",
  "amount": 1200.0000,
  "currency": "INR",
  "rawDescription": "UPI/ABC/123",
  "normalizedDescription": "ABC",
  "merchant": "ABC",
  "transactionType": "EXPENSE",
  "sourceRowReference": "p2r14"
}
```

The parser's debit/credit marker is consumed during classification and resolved into the transaction type. It is not persisted as a separate column, because the type already carries the ledger side (ADR-012).

## Rules
1. Normalize whitespace.
2. Preserve the raw description unchanged.
3. Normalize date formats to a calendar date with no timezone component.
4. Resolve the debit/credit marker into a ledger-side transaction type.
5. Use fixed-precision money, `NUMERIC(19,4)`.
6. Extract references only when reliably identifiable.
7. Normalize payment prefixes such as UPI, POS, NEFT, IMPS and ATM.
8. Never discard source row information.

## Classification

| Statement pattern | Type |
|---|---|
| Salary, credited income | `INCOME` |
| Merchant debit | `EXPENSE` |
| ATM withdrawal | `CASH_WITHDRAWAL` |
| Merchant reversal, returned purchase | `REFUND` |
| Bank or card fee charged | `FEE_CHARGED` |
| Interest charged on a card | `INTEREST_CHARGED` |
| Interest credited on a deposit | `INTEREST_EARNED` |
| Own-account debit | `TRANSFER_OUT` |
| Own-account credit | `TRANSFER_IN` |
| Card bill paid from a bank account | `CARD_PAYMENT_OUT` |
| Card bill received on the card account | `CARD_PAYMENT_IN` |
| Unresolved | `UNKNOWN` |

Interest and fees must be classified by direction, never by account type alone: bank accounts levy fees and card accounts refund them.

## Transfer Pairing
When a statement row is classified as a transfer or card payment and the counterpart account is tracked, the importer attempts to locate or create the matching half and assign a shared `transfer_group_id`. When the counterpart cannot be identified, the row is imported one-sided and flagged for review rather than silently dropped.

## Untrusted Text
Raw descriptions are attacker-influenced input. They are never concatenated into an AI prompt as instructions; see `04-ai/prompt-design.md`.

Unknown rows remain reviewable and never enter analytics.
