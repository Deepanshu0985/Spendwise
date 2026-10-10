package com.finance.domain.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** What has been spent against a budget in its current window. Categories without a limit are counted only in the total. */
public record BudgetProgress(
        LocalDate windowStart,
        LocalDate windowEnd,
        BigDecimal totalLimit,
        BigDecimal totalSpent,
        BigDecimal remaining,
        BigDecimal percentUsed,
        BudgetStatus status,
        List<CategoryProgress> categories) {

    public record CategoryProgress(
            UUID categoryId, BigDecimal limitAmount, BigDecimal spent, BigDecimal remaining, BigDecimal percentUsed, BudgetStatus status) {
    }
}
