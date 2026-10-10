package com.finance.domain.ai;

/** A model's answer for one row exactly as parsed from its JSON, before any of it is trusted. */
public record RawSuggestion(Integer index, String transactionType, String category, Double confidence, String reason) {
}
