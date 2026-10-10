package com.finance.application.ai;

import com.finance.domain.statement.StatementTransaction;

import java.util.List;

/**
 * rows: every row of the statement after the run. ruleApplied: filled by the free brand rules. aiApplied: filled from the
 * model at or above the confidence threshold. needsReview: sent to the model but left for the user (low confidence, no
 * confident category, or an answer that failed validation). notAsked: eligible rows beyond the per-request or daily limit.
 * stoppedReason: null, "UNAVAILABLE" (AI off or failing) or "LIMIT" (a usage cap); limitMessage explains a cap in words.
 */
public record CategorySuggestionResult(
        List<StatementTransaction> rows,
        int ruleApplied,
        int aiApplied,
        int needsReview,
        int notAsked,
        boolean aiAvailable,
        String stoppedReason,
        String limitMessage) {
}
