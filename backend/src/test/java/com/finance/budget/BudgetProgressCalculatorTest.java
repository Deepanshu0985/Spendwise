package com.finance.budget;

import com.finance.domain.budget.Budget;
import com.finance.domain.budget.BudgetCategoryLimit;
import com.finance.domain.budget.BudgetPeriodType;
import com.finance.domain.budget.BudgetProgress;
import com.finance.domain.budget.BudgetProgressCalculator;
import com.finance.domain.budget.BudgetStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetProgressCalculatorTest {

    private static final UUID FOOD = UUID.randomUUID();
    private static final UUID TRAVEL = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private static Budget monthly(String total, List<BudgetCategoryLimit> limits) {
        return new Budget(UUID.randomUUID(), "Monthly", BudgetPeriodType.MONTHLY, LocalDate.of(2026, 1, 1), null, new BigDecimal(total), "INR", limits);
    }

    private static Map<UUID, BigDecimal> spent(Object... pairs) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((UUID) pairs[i], new BigDecimal((String) pairs[i + 1]));
        }
        return map;
    }

    @Test
    void aMonthlyBudgetMeasuresTheCalendarMonthContainingToday() {
        BudgetProgress progress = BudgetProgressCalculator.calculate(monthly("10000", List.of()), TODAY, Map.of());

        assertThat(progress.windowStart()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(progress.windowEnd()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void aCustomBudgetMeasuresItsFixedRange() {
        Budget trip = new Budget(UUID.randomUUID(), "Goa trip", BudgetPeriodType.CUSTOM, LocalDate.of(2026, 12, 20),
                LocalDate.of(2026, 12, 28), new BigDecimal("30000"), "INR", List.of());

        BudgetProgress progress = BudgetProgressCalculator.calculate(trip, TODAY, Map.of());

        assertThat(progress.windowStart()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(progress.windowEnd()).isEqualTo(LocalDate.of(2026, 12, 28));
    }

    @Test
    void totalSpentIncludesCategoriesWithoutALimitAndUncategorisedSpending() {
        Budget budget = monthly("10000", List.of(new BudgetCategoryLimit(FOOD, new BigDecimal("4000"))));
        Map<UUID, BigDecimal> spent = spent(FOOD, "1000", TRAVEL, "2500");
        spent.put(null, new BigDecimal("500"));

        BudgetProgress progress = BudgetProgressCalculator.calculate(budget, TODAY, spent);

        assertThat(progress.totalSpent()).isEqualByComparingTo("4000");
        assertThat(progress.remaining()).isEqualByComparingTo("6000");
        assertThat(progress.percentUsed()).isEqualByComparingTo("40.0");
        assertThat(progress.status()).isEqualTo(BudgetStatus.ON_TRACK);
        assertThat(progress.categories()).hasSize(1);
        assertThat(progress.categories().get(0).spent()).isEqualByComparingTo("1000");
        assertThat(progress.categories().get(0).percentUsed()).isEqualByComparingTo("25.0");
    }

    @Test
    void aCategoryWithNoSpendingShowsZeroSpentAndFullRemaining() {
        Budget budget = monthly("10000", List.of(new BudgetCategoryLimit(TRAVEL, new BigDecimal("3000"))));

        BudgetProgress progress = BudgetProgressCalculator.calculate(budget, TODAY, Map.of());

        assertThat(progress.categories().get(0).spent()).isEqualByComparingTo("0");
        assertThat(progress.categories().get(0).remaining()).isEqualByComparingTo("3000");
    }

    @Test
    void statusIsOnTrackBelowEightyPercentCloseToLimitFromEightyAndOverOnlyAboveTheLimit() {
        Budget budget = monthly("1000", List.of());

        assertThat(BudgetProgressCalculator.calculate(budget, TODAY, spent(FOOD, "799")).status()).isEqualTo(BudgetStatus.ON_TRACK);
        assertThat(BudgetProgressCalculator.calculate(budget, TODAY, spent(FOOD, "800")).status()).isEqualTo(BudgetStatus.CLOSE_TO_LIMIT);
        assertThat(BudgetProgressCalculator.calculate(budget, TODAY, spent(FOOD, "1000")).status()).isEqualTo(BudgetStatus.CLOSE_TO_LIMIT);
        assertThat(BudgetProgressCalculator.calculate(budget, TODAY, spent(FOOD, "1000.01")).status()).isEqualTo(BudgetStatus.OVER_BUDGET);
    }

    @Test
    void overspendingGivesNegativeRemainingAndAPercentAboveAHundred() {
        Budget budget = monthly("10000", List.of(new BudgetCategoryLimit(FOOD, new BigDecimal("2000"))));

        BudgetProgress progress = BudgetProgressCalculator.calculate(budget, TODAY, spent(FOOD, "2500"));

        assertThat(progress.categories().get(0).remaining()).isEqualByComparingTo("-500");
        assertThat(progress.categories().get(0).percentUsed()).isEqualByComparingTo("125.0");
        assertThat(progress.categories().get(0).status()).isEqualTo(BudgetStatus.OVER_BUDGET);
        assertThat(progress.status()).isEqualTo(BudgetStatus.ON_TRACK);
    }
}
