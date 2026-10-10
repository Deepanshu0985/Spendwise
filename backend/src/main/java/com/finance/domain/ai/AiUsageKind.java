package com.finance.domain.ai;

/** What an allowance is spent on; each kind has its own daily and monthly cap. */
public enum AiUsageKind {
    /** Statement rows sent to the model for a category suggestion. */
    CATEGORIZATION,
    /** Messages sent to the assistant. */
    CHAT
}
