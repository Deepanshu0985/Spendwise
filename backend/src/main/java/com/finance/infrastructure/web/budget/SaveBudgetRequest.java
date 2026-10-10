package com.finance.infrastructure.web.budget;

import com.finance.domain.budget.BudgetPeriodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** startDate/endDate are required for CUSTOM budgets and ignored for MONTHLY; currency is required on create and ignored on update. */
public record SaveBudgetRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull BudgetPeriodType periodType,
        LocalDate startDate,
        LocalDate endDate,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal totalLimit,
        @Size(min = 3, max = 3) String currency,
        @Valid List<CategoryLimit> categoryLimits) {

    public record CategoryLimit(
            @NotNull UUID categoryId, @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal limitAmount) {
    }
}
