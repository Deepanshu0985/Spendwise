package com.finance.domain.statement;

/**
 * Real scoring (duplicate-detection.md's signals/tiers) is Phase 7 - this phase only
 * needs a place to record the outcome, defaulting every staged row to UNKNOWN.
 */
public enum DuplicateStatus {
    UNKNOWN,
    NOT_DUPLICATE,
    DUPLICATE
}
