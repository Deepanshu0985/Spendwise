package com.finance.infrastructure.web.analytics;

import com.finance.domain.analytics.CategoryBreakdownEntry;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryEntryResponse(UUID categoryId, String categoryName, BigDecimal amount) {

    public static CategoryEntryResponse from(CategoryBreakdownEntry entry) {
        return new CategoryEntryResponse(entry.categoryId(), entry.categoryName(), entry.amount());
    }
}
