package com.finance.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What the assistant's search tools ask the repository for. Only CONFIRMED, non-deleted transactions are ever returned.
 * Every word in textMatchers must match: a word matches if it appears in the description or raw description, or if the
 * transaction's merchant is one of the merchantIds whose name contains that word.
 */
public record TransactionLookup(
        LocalDate from,
        LocalDate to,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        TransactionType type,
        UUID categoryId,
        Set<UUID> merchantIds,
        List<TextMatcher> textMatchers) {

    public record TextMatcher(String word, Set<UUID> merchantIdsWhoseNameContainsIt) {
    }

    public enum Sort {
        DATE_DESC,
        DATE_ASC,
        AMOUNT_DESC,
        AMOUNT_ASC
    }
}
