package com.finance.domain.statement;

public enum DuplicateReason {
    EXACT_REFERENCE,
    DATE_AMOUNT_DESCRIPTION,
    NEARBY_SIMILAR,
    /** Same amount (within rounding) and a day either side of a transaction the user typed in by hand, in any account. */
    MANUAL_ENTRY_MATCH
}
