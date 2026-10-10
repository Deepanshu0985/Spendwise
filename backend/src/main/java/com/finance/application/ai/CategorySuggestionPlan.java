package com.finance.application.ai;

import com.finance.domain.ai.AiRowInput;
import com.finance.domain.category.Category;

import java.util.List;

/** What is left for the model after the free rules ran: the rows to ask about and the categories it may choose from. */
public record CategorySuggestionPlan(int ruleApplied, List<AiRowInput> rowsToAsk, int eligibleBeyondLimit, List<Category> categories) {
}
