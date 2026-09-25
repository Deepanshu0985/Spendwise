package com.finance.domain.analytics;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code categoryId} is null for the reserved "Uncategorized" bucket (analytics-specification.md's Category Aggregation). */
public record CategoryBreakdownEntry(UUID categoryId, String categoryName, BigDecimal amount) {
}
