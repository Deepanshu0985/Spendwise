package com.finance.domain.analytics;

import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionSplit;
import com.finance.domain.transaction.TransactionType;
import com.finance.domain.transaction.TransactionWithSplits;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Every figure the product displays, computed deterministically from a plain
 * list of transactions - no persistence, no framework, so the six
 * reconciliation invariants in analytics-specification.md can be asserted as
 * pure unit tests against the September worked example fixture. Pure
 * query/aggregation logic, not swappable business behavior - no interface,
 * same reasoning as TransactionSpecifications.
 *
 * Callers are responsible for the two filters analytics-specification.md's
 * Inclusion Rules require before calling any method here: {@code status =
 * CONFIRMED}, {@code transaction_type != UNKNOWN}, and for currency scoping -
 * every transaction passed in is assumed to already be in the one requested
 * currency (see AnalyticsServiceImpl, which does the filtering and reports the
 * exclusion separately).
 */
public final class AnalyticsCalculator {

    private static final String UNCATEGORIZED = "Uncategorized";
    private static final String UNKNOWN_MERCHANT = "Unknown merchant";

    private AnalyticsCalculator() {
    }

    public static PeriodFigures figures(List<TransactionWithSplits> transactions) {
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO;
        for (TransactionWithSplits tws : transactions) {
            Transaction t = tws.transaction();
            expenses = expenses.add(expenseContribution(t.getTransactionType(), t.getAmount()));
            income = income.add(incomeContribution(t.getTransactionType(), t.getAmount()));
        }
        return PeriodFigures.of(expenses, income);
    }

    /** categoryId is null for the reserved "Uncategorized" bucket. Splits, when present, replace the parent transaction's own category allocation entirely (transaction_category_allocations' UNION ALL shape). */
    public static List<CategoryBreakdownEntry> categoryBreakdown(List<TransactionWithSplits> transactions, Map<UUID, String> categoryNames) {
        Map<UUID, BigDecimal> totals = new LinkedHashMap<>();
        for (TransactionWithSplits tws : transactions) {
            Transaction t = tws.transaction();
            BigDecimal sign = expenseSign(t.getTransactionType());
            if (sign.signum() == 0) {
                continue;
            }
            if (tws.splits().isEmpty()) {
                totals.merge(t.getCategoryId(), sign.multiply(t.getAmount()), BigDecimal::add);
            } else {
                for (TransactionSplit split : tws.splits()) {
                    totals.merge(split.getCategoryId(), sign.multiply(split.getAmount()), BigDecimal::add);
                }
            }
        }
        return totals.entrySet().stream()
                .map(e -> new CategoryBreakdownEntry(e.getKey(), nameOrDefault(categoryNames, e.getKey(), UNCATEGORIZED), e.getValue()))
                .toList();
    }

    /** merchantId is null for the reserved "Unknown merchant" bucket. Always the full transaction amount - merchant is a transaction-level attribute, splits don't carry one. */
    public static List<MerchantBreakdownEntry> merchantBreakdown(List<TransactionWithSplits> transactions, Map<UUID, String> merchantNames) {
        Map<UUID, BigDecimal> totals = new LinkedHashMap<>();
        for (TransactionWithSplits tws : transactions) {
            Transaction t = tws.transaction();
            BigDecimal sign = expenseSign(t.getTransactionType());
            if (sign.signum() == 0) {
                continue;
            }
            totals.merge(t.getMerchantId(), sign.multiply(t.getAmount()), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(e -> new MerchantBreakdownEntry(e.getKey(), nameOrDefault(merchantNames, e.getKey(), UNKNOWN_MERCHANT), e.getValue()))
                .toList();
    }

    // Map.of()'s immutable maps throw NPE on get(null) rather than returning
    // null - checking the null key ourselves first works regardless of which
    // Map implementation a caller (or a test) passes in.
    private static String nameOrDefault(Map<UUID, String> names, UUID id, String fallback) {
        return id == null ? fallback : names.getOrDefault(id, fallback);
    }

    /** One point per calendar month in [from, to], zero-filled so a chart stays continuous even where there's no data. */
    public static List<TrendPoint> trend(List<TransactionWithSplits> transactions, YearMonth from, YearMonth to) {
        Map<YearMonth, List<TransactionWithSplits>> byMonth = transactions.stream()
                .collect(Collectors.groupingBy(tws -> YearMonth.from(tws.transaction().getTransactionDate())));

        List<TrendPoint> points = new ArrayList<>();
        for (YearMonth month = from; !month.isAfter(to); month = month.plusMonths(1)) {
            points.add(new TrendPoint(month, figures(byMonth.getOrDefault(month, List.of()))));
        }
        return points;
    }

    // Positive for expense-side types, negative for REFUND (which reduces
    // expenses), zero for everything else - income, transfers, card payments
    // and cash withdrawals never touch a category/merchant expense breakdown
    // (analytics-specification.md's Transaction Types and Ledger Sides table).
    private static BigDecimal expenseSign(TransactionType type) {
        return switch (type) {
            case EXPENSE, FEE_CHARGED, INTEREST_CHARGED -> BigDecimal.ONE;
            case REFUND -> BigDecimal.ONE.negate();
            default -> BigDecimal.ZERO;
        };
    }

    private static BigDecimal expenseContribution(TransactionType type, BigDecimal amount) {
        return expenseSign(type).multiply(amount);
    }

    private static BigDecimal incomeContribution(TransactionType type, BigDecimal amount) {
        return switch (type) {
            case INCOME, INTEREST_EARNED -> amount;
            default -> BigDecimal.ZERO;
        };
    }
}
