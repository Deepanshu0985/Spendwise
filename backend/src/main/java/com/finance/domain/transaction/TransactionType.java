package com.finance.domain.transaction;

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
    UNKNOWN;

    /** True for types that take money out; false for types that bring money in. Meaningless for UNKNOWN - check isUnknown() first. */
    public boolean isDebitSide() {
        return switch (this) {
            case EXPENSE, FEE_CHARGED, INTEREST_CHARGED, TRANSFER_OUT, CARD_PAYMENT_OUT, CASH_WITHDRAWAL -> true;
            case INCOME, REFUND, INTEREST_EARNED, TRANSFER_IN, CARD_PAYMENT_IN, UNKNOWN -> false;
        };
    }

    public boolean isUnknown() {
        return this == UNKNOWN;
    }
}
