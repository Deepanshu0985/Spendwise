package com.finance.infrastructure.web.budget;

import com.finance.application.budget.BudgetView;
import com.finance.domain.budget.Budget;
import com.finance.domain.budget.BudgetPeriodType;
import com.finance.domain.budget.BudgetProgress;
import com.finance.domain.budget.BudgetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        String name,
        BudgetPeriodType periodType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalLimit,
        String currency,
        Progress progress) {

    public record Progress(
            LocalDate windowStart,
            LocalDate windowEnd,
            BigDecimal totalSpent,
            BigDecimal remaining,
            BigDecimal percentUsed,
            BudgetStatus status,
            List<CategoryProgress> categories) {
    }

    public record CategoryProgress(
            UUID categoryId, BigDecimal limitAmount, BigDecimal spent, BigDecimal remaining, BigDecimal percentUsed, BudgetStatus status) {
    }

    public static BudgetResponse from(BudgetView view) {
        Budget budget = view.budget();
        BudgetProgress p = view.progress();
        return new BudgetResponse(
                budget.getId(), budget.getName(), budget.getPeriodType(), budget.getStartDate(), budget.getEndDate(),
                budget.getTotalLimit(), budget.getCurrency(),
                new Progress(
                        p.windowStart(), p.windowEnd(), p.totalSpent(), p.remaining(), p.percentUsed(), p.status(),
                        p.categories().stream()
                                .map(c -> new CategoryProgress(c.categoryId(), c.limitAmount(), c.spent(), c.remaining(), c.percentUsed(), c.status()))
                                .toList()));
    }
}
