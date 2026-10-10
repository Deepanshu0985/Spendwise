package com.finance.domain.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Framework-free: turns a budget and the spending per category in its window into progress figures. */
public final class BudgetProgressCalculator {

    /** At or above this share of a limit the budget is "close to limit". */
    static final BigDecimal WARNING_SHARE = new BigDecimal("80");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private BudgetProgressCalculator() {
    }

    /**
     * @param spentByCategory expense totals in the budget's currency for the window, keyed by category id; the key
     *                        null holds uncategorised spending, which counts toward the total only
     */
    public static BudgetProgress calculate(Budget budget, LocalDate today, Map<UUID, BigDecimal> spentByCategory) {
        LocalDate[] window = budget.windowAsOf(today);
        BigDecimal totalSpent = spentByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BudgetProgress.CategoryProgress> categories = budget.getCategoryLimits().stream()
                .map(limit -> {
                    BigDecimal spent = spentByCategory.getOrDefault(limit.categoryId(), BigDecimal.ZERO);
                    return new BudgetProgress.CategoryProgress(
                            limit.categoryId(), limit.limitAmount(), spent, limit.limitAmount().subtract(spent),
                            percent(spent, limit.limitAmount()), statusFor(spent, limit.limitAmount()));
                })
                .toList();
        return new BudgetProgress(
                window[0], window[1], budget.getTotalLimit(), totalSpent, budget.getTotalLimit().subtract(totalSpent),
                percent(totalSpent, budget.getTotalLimit()), statusFor(totalSpent, budget.getTotalLimit()), categories);
    }

    static BigDecimal percent(BigDecimal spent, BigDecimal limit) {
        return spent.multiply(HUNDRED).divide(limit, 1, RoundingMode.HALF_UP);
    }

    static BudgetStatus statusFor(BigDecimal spent, BigDecimal limit) {
        BigDecimal percent = spent.multiply(HUNDRED).divide(limit, 4, RoundingMode.HALF_UP);
        if (percent.compareTo(HUNDRED) > 0) {
            return BudgetStatus.OVER_BUDGET;
        }
        return percent.compareTo(WARNING_SHARE) >= 0 ? BudgetStatus.CLOSE_TO_LIMIT : BudgetStatus.ON_TRACK;
    }
}
