package com.finance.domain.statement;

/** pdf-processing.md's state machine: UPLOADED -> PROCESSING -> READY_FOR_REVIEW -> IMPORTED, or PROCESSING -> FAILED. */
public enum StatementStatus {
    UPLOADED,
    PROCESSING,
    READY_FOR_REVIEW,
    IMPORTED,
    FAILED
}
