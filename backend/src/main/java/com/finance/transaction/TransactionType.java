package com.finance.transaction;

/** Ledger-side mapping is the authoritative contract in analytics-specification.md - type alone determines side (ADR-012). */
public enum TransactionType {
    EXPENSE,
    INCOME,
    REFUND,
    FEE_CHARGED,
    INTEREST_CHARGED,
    INTEREST_EARNED,
    TRANSFER_OUT,
    TRANSFER_IN,
    CARD_PAYMENT_OUT,
    CARD_PAYMENT_IN,
    CASH_WITHDRAWAL,
    UNKNOWN
}
