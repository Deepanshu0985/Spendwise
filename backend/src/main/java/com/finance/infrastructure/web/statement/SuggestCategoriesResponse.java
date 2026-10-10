package com.finance.infrastructure.web.statement;

import com.finance.application.ai.CategorySuggestionResult;

import java.util.List;

public record SuggestCategoriesResponse(
        List<StatementTransactionResponse> rows,
        int ruleApplied,
        int aiApplied,
        int needsReview,
        int notAsked,
        boolean aiAvailable,
        String stoppedReason,
        String limitMessage) {

    public static SuggestCategoriesResponse from(CategorySuggestionResult result) {
        return new SuggestCategoriesResponse(
                result.rows().stream().map(StatementTransactionResponse::from).toList(), result.ruleApplied(), result.aiApplied(),
                result.needsReview(), result.notAsked(), result.aiAvailable(), result.stoppedReason(), result.limitMessage());
    }
}
