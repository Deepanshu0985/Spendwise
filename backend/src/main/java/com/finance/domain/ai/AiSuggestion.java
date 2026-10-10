package com.finance.domain.ai;

import com.finance.domain.transaction.TransactionType;

import java.util.UUID;

/** A suggestion that passed validation: a real row, a ledger type, one of the user's own categories, a sane confidence. */
public record AiSuggestion(UUID rowId, TransactionType type, UUID categoryId, double confidence, String reason) {
}
