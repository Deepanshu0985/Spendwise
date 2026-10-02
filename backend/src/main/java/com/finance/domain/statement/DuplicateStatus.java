package com.finance.domain.statement;

/** Outcome of duplicate-detection.md's scoring for a staged row. */
public enum DuplicateStatus {
    UNKNOWN,
    NOT_DUPLICATE,
    /** Medium evidence: shown for review, still imported unless the user rejects it. */
    POSSIBLE_DUPLICATE,
    /** Strong evidence: skipped on confirm unless the user explicitly keeps it. */
    DUPLICATE
}
