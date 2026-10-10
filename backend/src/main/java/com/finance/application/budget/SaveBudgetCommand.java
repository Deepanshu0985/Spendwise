package com.finance.application.budget;

import com.finance.domain.budget.BudgetPeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** startDate and endDate are only used for CUSTOM budgets. currency is only used on create (a budget's currency never changes). */
public record SaveBudgetCommand(
        String name, BudgetPeriodType periodType, LocalDate startDate, LocalDate endDate, BigDecimal totalLimit, String currency,
        List<BudgetCategoryLimitCommand> categoryLimits) {
}
