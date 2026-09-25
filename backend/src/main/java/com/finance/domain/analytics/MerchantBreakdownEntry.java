package com.finance.domain.analytics;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code merchantId} is null for the reserved "Unknown merchant" bucket (analytics-specification.md's Category Aggregation). */
public record MerchantBreakdownEntry(UUID merchantId, String merchantName, BigDecimal amount) {
}
