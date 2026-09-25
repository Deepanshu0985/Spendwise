package com.finance.analytics;

import com.finance.domain.analytics.AnalyticsCalculator;
import com.finance.domain.analytics.CategoryBreakdownEntry;
import com.finance.domain.analytics.MerchantBreakdownEntry;
import com.finance.domain.analytics.PeriodFigures;
import com.finance.domain.analytics.TrendPoint;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionSplit;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;
import com.finance.domain.transaction.TransactionWithSplits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The six reconciliation invariants from analytics-specification.md, each as
 * its own test, plus the September worked example as the required fixture
 * (it alone exercises every rule in the document at once). Pure in-memory -
 * AnalyticsCalculator has no persistence or framework dependency, so none of
 * this needs a database.
 */
class AnalyticsCalculatorTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CARD_ACCOUNT = UUID.randomUUID();
    private static final UUID BANK_ACCOUNT = UUID.randomUUID();
    private static final UUID SWIGGY_MERCHANT = UUID.randomUUID();
    private static final UUID GROCERIES_MERCHANT = UUID.randomUUID();
    private static final UUID FOOD_CATEGORY = UUID.randomUUID();
    private static final UUID GROCERIES_CATEGORY = UUID.randomUUID();

    private List<TransactionWithSplits> worked;
    private PeriodFigures figures;
    private List<CategoryBreakdownEntry> categoryBreakdown;
    private List<MerchantBreakdownEntry> merchantBreakdown;
    private List<TrendPoint> trend;

    @BeforeEach
    void setUp() {
        worked = septemberWorkedExample();
        figures = AnalyticsCalculator.figures(worked);
        categoryBreakdown = AnalyticsCalculator.categoryBreakdown(worked, categoryNames());
        merchantBreakdown = AnalyticsCalculator.merchantBreakdown(worked, merchantNames());
        trend = AnalyticsCalculator.trend(worked, YearMonth.of(2026, 9), YearMonth.of(2026, 9));
    }

    @Test
    void matchesTheWorkedExampleExactly() {
        // expenses = (500 + 2000) - 500 = 2,000. income = 80,000. savings = 78,000. savings_rate = 97.5.
        assertThat(figures.expenses()).isEqualByComparingTo("2000");
        assertThat(figures.income()).isEqualByComparingTo("80000");
        assertThat(figures.savings()).isEqualByComparingTo("78000");
        assertThat(figures.savingsRate()).isEqualByComparingTo("97.5");
    }

    @Test
    void invariant1_categoryBreakdownSumsToExpenses() {
        BigDecimal sum = categoryBreakdown.stream().map(CategoryBreakdownEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(figures.expenses());
    }

    @Test
    void invariant2_merchantBreakdownSumsToExpenses() {
        BigDecimal sum = merchantBreakdown.stream().map(MerchantBreakdownEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(figures.expenses());
    }

    @Test
    void invariant3_trendSeriesSumsToThePeriodTotal() {
        BigDecimal trendExpenses = trend.stream().map(p -> p.figures().expenses()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal trendIncome = trend.stream().map(p -> p.figures().income()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(trendExpenses).isEqualByComparingTo(figures.expenses());
        assertThat(trendIncome).isEqualByComparingTo(figures.income());
    }

    @Test
    void invariant4_splitAllocationsSumToTheTransactionAmount() {
        UUID diningId = UUID.randomUUID();
        UUID entertainmentId = UUID.randomUUID();
        Transaction split = transaction(CARD_ACCOUNT, null, null, "2026-09-10", "1000", TransactionType.EXPENSE);
        List<TransactionSplit> splits = List.of(
                new TransactionSplit(USER_ID, split.getId(), diningId, new BigDecimal("600")),
                new TransactionSplit(USER_ID, split.getId(), entertainmentId, new BigDecimal("400")));
        BigDecimal splitSum = splits.stream().map(TransactionSplit::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(splitSum).isEqualByComparingTo(split.getAmount());

        // And the calculator allocates to the splits' categories, not the (null) parent category.
        List<TransactionWithSplits> withSplit = List.of(new TransactionWithSplits(split, splits));
        Map<UUID, String> names = Map.of(diningId, "Dining", entertainmentId, "Entertainment");
        List<CategoryBreakdownEntry> breakdown = AnalyticsCalculator.categoryBreakdown(withSplit, names);
        BigDecimal total = breakdown.stream().map(CategoryBreakdownEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("1000");
        assertThat(breakdown).extracting(CategoryBreakdownEntry::categoryId).containsExactlyInAnyOrder(diningId, entertainmentId);
    }

    @Test
    void invariant5_incomeMinusExpensesEqualsSavings() {
        assertThat(figures.income().subtract(figures.expenses())).isEqualByComparingTo(figures.savings());
    }

    @Test
    void invariant6_transferGroupIsEqualInMagnitudeAndContributesZero() {
        Transaction out = worked.stream().map(TransactionWithSplits::transaction)
                .filter(t -> t.getTransactionType() == TransactionType.CARD_PAYMENT_OUT).findFirst().orElseThrow();
        Transaction in = worked.stream().map(TransactionWithSplits::transaction)
                .filter(t -> t.getTransactionType() == TransactionType.CARD_PAYMENT_IN).findFirst().orElseThrow();

        assertThat(out.getAmount()).isEqualByComparingTo(in.getAmount());
        assertThat(out.getTransferGroupId()).isEqualTo(in.getTransferGroupId());

        PeriodFigures transferOnly = AnalyticsCalculator.figures(
                List.of(new TransactionWithSplits(out, List.of()), new TransactionWithSplits(in, List.of())));
        assertThat(transferOnly.expenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(transferOnly.income()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /** analytics-specification.md's Worked Example - one credit card and one bank account, September. */
    private List<TransactionWithSplits> septemberWorkedExample() {
        UUID transferGroup = UUID.randomUUID();

        Transaction swiggy = transaction(CARD_ACCOUNT, SWIGGY_MERCHANT, FOOD_CATEGORY, "2026-09-03", "500", TransactionType.EXPENSE);
        Transaction groceries = transaction(CARD_ACCOUNT, GROCERIES_MERCHANT, GROCERIES_CATEGORY, "2026-09-08", "2000", TransactionType.EXPENSE);
        Transaction cardBillOut = transaction(BANK_ACCOUNT, null, null, "2026-09-20", "2500", TransactionType.CARD_PAYMENT_OUT);
        cardBillOut.setTransferGroupId(transferGroup);
        Transaction cardBillIn = transaction(CARD_ACCOUNT, null, null, "2026-09-20", "2500", TransactionType.CARD_PAYMENT_IN);
        cardBillIn.setTransferGroupId(transferGroup);
        Transaction refund = transaction(CARD_ACCOUNT, SWIGGY_MERCHANT, FOOD_CATEGORY, "2026-09-25", "500", TransactionType.REFUND);
        Transaction salary = transaction(BANK_ACCOUNT, null, null, "2026-09-30", "80000", TransactionType.INCOME);

        return List.of(swiggy, groceries, cardBillOut, cardBillIn, refund, salary).stream()
                .map(t -> new TransactionWithSplits(t, List.of()))
                .toList();
    }

    private Transaction transaction(UUID accountId, UUID merchantId, UUID categoryId, String date, String amount, TransactionType type) {
        return new Transaction(
                USER_ID, accountId, merchantId, categoryId, LocalDate.parse(date), new BigDecimal(amount), "INR", "test",
                type, TransactionSource.MANUAL, TransactionStatus.CONFIRMED);
    }

    private Map<UUID, String> categoryNames() {
        return Map.of(FOOD_CATEGORY, "Food & Dining", GROCERIES_CATEGORY, "Groceries");
    }

    private Map<UUID, String> merchantNames() {
        return Map.of(SWIGGY_MERCHANT, "Swiggy", GROCERIES_MERCHANT, "Groceries Store");
    }
}
