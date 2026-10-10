package com.finance.application.ai;

import com.finance.domain.ai.AiSuggestion;
import com.finance.domain.statement.StatementTransaction;

import java.util.List;
import java.util.UUID;

/**
 * The database side of AI categorisation, kept separate from the orchestrator so each step is its own short transaction:
 * no transaction is ever held open while waiting on the model.
 */
public interface CategorySuggestionStore {

    /** Applies the free brand rules, then returns the rows still needing a category (at most maxRows) for the model. */
    CategorySuggestionPlan prepare(UUID userId, UUID statementId, boolean forModel, int maxRows);

    /**
     * Applies suggestions at or above the threshold to rows that are still unreviewed and uncategorised, and returns how
     * many were applied. Suggestions below the threshold leave the row exactly as it was.
     */
    int apply(UUID userId, UUID statementId, List<AiSuggestion> suggestions, double threshold);

    List<StatementTransaction> rows(UUID userId, UUID statementId);
}
