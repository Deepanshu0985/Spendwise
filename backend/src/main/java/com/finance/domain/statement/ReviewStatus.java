package com.finance.domain.statement;

/** A staged statement_transactions row's review state before/after the user looks at it. */
public enum ReviewStatus {
    PENDING,
    ACCEPTED,
    EDITED,
    REJECTED
}
