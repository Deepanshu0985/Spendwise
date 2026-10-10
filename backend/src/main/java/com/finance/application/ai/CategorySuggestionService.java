package com.finance.application.ai;

import java.util.UUID;

public interface CategorySuggestionService {

    /**
     * Suggests categories for a statement's uncategorised, unreviewed rows: the free brand rules first, then - if AI is
     * available and the caps allow - the model for what is left. Never changes a row the user has reviewed.
     */
    CategorySuggestionResult suggest(UUID userId, UUID statementId);

    /** Whether AI is available at all, and how many more rows this user may send today. */
    AiStatus status(UUID userId);

    record AiStatus(boolean enabled, int remainingRowsToday, int dailyLimit, int remainingMessagesToday, int dailyMessageLimit) {
    }
}
