package com.finance.application.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.UntrustedText;
import com.finance.application.analytics.AnalyticsService;
import com.finance.application.analytics.CategoryBreakdownView;
import com.finance.application.analytics.MerchantBreakdownView;
import com.finance.application.analytics.MonthlySummaryView;
import com.finance.application.budget.BudgetService;
import com.finance.application.budget.BudgetView;
import com.finance.domain.analytics.CategoryBreakdownEntry;
import com.finance.domain.analytics.MerchantBreakdownEntry;
import com.finance.domain.insight.UnusualSpendingDetector;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionLookup;
import com.finance.domain.transaction.TransactionRepository;
import com.finance.domain.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InsightMetricsBuilderImpl implements InsightMetricsBuilder {

    /** Bumped whenever the metrics' shape or the wording rules change, so older stored text is regenerated. */
    public static final String METRICS_VERSION = "metrics-v2";

    /** Exact decimals: without this Jackson would write 12000.00 as 1.2E+4, which the model and the figure check should never see. */
    private static final com.fasterxml.jackson.databind.node.JsonNodeFactory JSON = com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true);

    private static final int HISTORY_MONTHS = 3;
    private static final int PAYMENT_HISTORY_MONTHS = 6;
    private static final int MAX_PAYMENTS_READ = 5_000;
    private static final int TOP_ITEMS = 3;

    private final AnalyticsService analyticsService;
    private final BudgetService budgetService;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper mapper;
    private final Clock clock;

    public InsightMetricsBuilderImpl(
            AnalyticsService analyticsService, BudgetService budgetService, TransactionRepository transactionRepository, ObjectMapper mapper, Clock clock) {
        this.analyticsService = analyticsService;
        this.budgetService = budgetService;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public InsightMetrics build(UUID userId, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        MonthlySummaryView summary = analyticsService.monthlySummary(userId, from, to, null);
        String currency = summary.currency();
        BigDecimal income = summary.figures().income();
        BigDecimal expenses = summary.figures().expenses();

        ObjectNode json = JSON.objectNode();
        json.put("month", month.toString());
        json.put("currency", currency);
        ObjectNode period = json.putObject("period");
        period.put("from", from.toString());
        period.put("to", to.toString());
        json.put("income", money(income));
        json.put("expenses", money(expenses));
        json.put("savings", money(summary.figures().savings()));
        if (summary.figures().savingsRate() == null) {
            json.putNull("savingsRate");
        } else {
            json.put("savingsRate", summary.figures().savingsRate().setScale(1, RoundingMode.HALF_UP));
        }
        boolean hasActivity = income.signum() != 0 || expenses.signum() != 0;

        // Comparison with the month before: the difference and percentage are worked out here, never by the model.
        YearMonth previous = month.minusMonths(1);
        MonthlySummaryView before = analyticsService.monthlySummary(userId, previous.atDay(1), previous.atEndOfMonth(), currency);
        if (before.figures().income().signum() != 0 || before.figures().expenses().signum() != 0) {
            ObjectNode prev = json.putObject("previousMonth");
            prev.put("month", previous.toString());
            prev.put("income", money(before.figures().income()));
            prev.put("expenses", money(before.figures().expenses()));
            prev.put("savings", money(before.figures().savings()));
            ObjectNode change = json.putObject("expensesChange");
            BigDecimal difference = expenses.subtract(before.figures().expenses());
            change.put("difference", money(difference));
            if (before.figures().expenses().signum() > 0) {
                change.put("percent", difference.multiply(BigDecimal.valueOf(100)).divide(before.figures().expenses(), 1, RoundingMode.HALF_UP));
            } else {
                change.putNull("percent");
            }
            // The other differences a sentence naturally reaches for, worked out here so the model never has to subtract.
            ObjectNode incomeChange = json.putObject("incomeChange");
            BigDecimal incomeDifference = income.subtract(before.figures().income());
            incomeChange.put("difference", money(incomeDifference));
            if (before.figures().income().signum() > 0) {
                incomeChange.put("percent", incomeDifference.multiply(BigDecimal.valueOf(100)).divide(before.figures().income(), 1, RoundingMode.HALF_UP));
            } else {
                incomeChange.putNull("percent");
            }
            json.putObject("savingsChange").put("difference", money(summary.figures().savings().subtract(before.figures().savings())));
        }

        CategoryBreakdownView categories = analyticsService.categoryBreakdown(userId, from, to, currency);
        ArrayNode topCategories = json.putArray("topCategories");
        categories.entries().stream().filter(e -> e.amount().signum() > 0).sorted(Comparator.comparing(CategoryBreakdownEntry::amount).reversed()).limit(TOP_ITEMS)
                .forEach(entry -> {
                    ObjectNode node = topCategories.addObject();
                    node.put("name", UntrustedText.of(entry.categoryName()));
                    node.put("amount", money(entry.amount()));
                    if (expenses.signum() > 0) {
                        node.put("sharePercent", entry.amount().multiply(BigDecimal.valueOf(100)).divide(expenses, 1, RoundingMode.HALF_UP));
                    }
                });

        MerchantBreakdownView merchants = analyticsService.merchantBreakdown(userId, from, to, currency);
        ArrayNode topMerchants = json.putArray("topMerchants");
        merchants.entries().stream().filter(e -> e.amount().signum() > 0 && e.merchantId() != null)
                .sorted(Comparator.comparing(MerchantBreakdownEntry::amount).reversed()).limit(TOP_ITEMS)
                .forEach(entry -> {
                    ObjectNode node = topMerchants.addObject();
                    node.put("name", UntrustedText.of(entry.merchantName()));
                    node.put("amount", money(entry.amount()));
                });

        ArrayNode unusual = json.putArray("unusual");
        if (hasActivity) {
            for (UnusualSpendingDetector.Unusual item : detectUnusual(userId, month, currency, categories)) {
                ObjectNode node = unusual.addObject();
                node.put("kind", item.kind().name());
                node.put("label", UntrustedText.of(item.label()));
                node.put("amount", item.amount());
                node.put("typical", item.typical());
                node.put("increase", item.increase());
                node.put("timesTypical", item.timesTypical());
                if (item.date() != null) {
                    node.put("date", item.date().toString());
                }
            }
        }

        // Budgets describe the current period, so they are only relevant to the month that is still running.
        ArrayNode budgets = json.putArray("budgets");
        if (month.equals(YearMonth.now(clock))) {
            for (BudgetView view : budgetService.list(userId)) {
                if (!view.budget().getCurrency().equals(currency)) {
                    continue;
                }
                ObjectNode node = budgets.addObject();
                node.put("name", UntrustedText.of(view.budget().getName()));
                node.put("limit", money(view.budget().getTotalLimit()));
                node.put("spent", money(view.progress().totalSpent()));
                node.put("percentUsed", view.progress().percentUsed());
                node.put("status", view.progress().status().name());
                // Remaining and over-by are worked out here: "exceeded by" is the first thing a sentence about a budget wants to say.
                node.put("remaining", money(view.budget().getTotalLimit().subtract(view.progress().totalSpent())));
                if (view.progress().totalSpent().compareTo(view.budget().getTotalLimit()) > 0) {
                    node.put("overBy", money(view.progress().totalSpent().subtract(view.budget().getTotalLimit())));
                    node.put("percentOver", view.progress().percentUsed().subtract(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
                }
            }
        }
        return new InsightMetrics(json, hash(json.toString()), hasActivity);
    }

    private List<UnusualSpendingDetector.Unusual> detectUnusual(UUID userId, YearMonth month, String currency, CategoryBreakdownView currentCategories) {
        Map<String, BigDecimal> current = new LinkedHashMap<>();
        currentCategories.entries().stream().filter(e -> e.amount().signum() > 0).forEach(e -> current.merge(e.categoryName(), e.amount(), BigDecimal::add));

        List<Map<String, BigDecimal>> previous = new ArrayList<>();
        List<BigDecimal> monthlyTotals = new ArrayList<>();
        for (int i = HISTORY_MONTHS; i >= 1; i--) {
            YearMonth earlier = month.minusMonths(i);
            Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
            for (CategoryBreakdownEntry entry : analyticsService.categoryBreakdown(userId, earlier.atDay(1), earlier.atEndOfMonth(), currency).entries()) {
                if (entry.amount().signum() > 0) {
                    byCategory.merge(entry.categoryName(), entry.amount(), BigDecimal::add);
                }
            }
            BigDecimal total = byCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.signum() > 0) {
                previous.add(byCategory);
                monthlyTotals.add(total);
            }
        }
        if (monthlyTotals.isEmpty()) {
            return List.of();
        }
        BigDecimal average = monthlyTotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(monthlyTotals.size()), 2, RoundingMode.HALF_UP);

        List<UnusualSpendingDetector.Payment> payments = expensePayments(userId, month.atDay(1), month.atEndOfMonth(), currency).stream()
                .map(t -> new UnusualSpendingDetector.Payment(describe(t), t.getTransactionDate(), t.getAmount())).toList();
        List<UnusualSpendingDetector.Payment> history = expensePayments(userId, month.minusMonths(PAYMENT_HISTORY_MONTHS).atDay(1), month.minusMonths(1).atEndOfMonth(), currency).stream()
                .map(t -> new UnusualSpendingDetector.Payment(describe(t), t.getTransactionDate(), t.getAmount())).toList();
        return UnusualSpendingDetector.detect(current, previous, average, payments, history);
    }

    private List<Transaction> expensePayments(UUID userId, LocalDate from, LocalDate to, String currency) {
        return transactionRepository.lookup(userId, new TransactionLookup(from, to, null, null, TransactionType.EXPENSE, null, null, List.of()),
                TransactionLookup.Sort.DATE_DESC, MAX_PAYMENTS_READ).stream().filter(t -> currency.equals(t.getCurrency())).toList();
    }

    private static String describe(Transaction t) {
        return t.getDescription() != null && !t.getDescription().isBlank() ? t.getDescription() : (t.getRawDescription() == null ? "A payment" : t.getRawDescription());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String hash(String metricsJson) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((metricsJson + "|" + METRICS_VERSION + "|" + ModelInsightWriter.PROMPT_VERSION + "|" + TemplateInsightWriter.TEMPLATE_VERSION)
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
