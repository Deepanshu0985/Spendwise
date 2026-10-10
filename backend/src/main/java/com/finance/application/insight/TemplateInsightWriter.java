package com.finance.application.insight;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Writes an insight from the metrics with fixed sentences and no model. It is what is shown when AI is off, over its caps,
 * unavailable or its wording could not be verified, so the insight is never empty and never depends on a provider. Every figure it
 * writes is read straight from the metrics (an absolute value where the sentence supplies the direction), so it is grounded by construction.
 */
@Component("templateInsightWriter")
public class TemplateInsightWriter implements InsightWriter {

    public static final String TEMPLATE_VERSION = "template-v2";
    private static final int MAX_HIGHLIGHTS = 6;

    @Override
    public Optional<InsightText> write(JsonNode m) {
        YearMonth month = YearMonth.parse(m.get("month").asText());
        String name = monthName(month);
        String currency = m.get("currency").asText();
        BigDecimal income = m.get("income").decimalValue();
        BigDecimal expenses = m.get("expenses").decimalValue();
        BigDecimal savings = m.get("savings").decimalValue();

        if (income.signum() == 0 && expenses.signum() == 0) {
            return Optional.of(new InsightText(name + " at a glance", "No income or spending was recorded for " + name + ".", List.of()));
        }

        StringBuilder summary = new StringBuilder("In ").append(name).append(" you earned ").append(money(currency, income))
                .append(" and spent ").append(money(currency, expenses)).append(". ");
        if (savings.signum() >= 0) {
            summary.append("That left ").append(money(currency, savings));
            if (!m.path("savingsRate").isNull() && m.has("savingsRate")) {
                summary.append(", a savings rate of ").append(m.get("savingsRate").decimalValue().toPlainString()).append("% of your income");
            }
            summary.append(".");
        } else {
            summary.append("You spent ").append(money(currency, savings.abs())).append(" more than you earned.");
        }
        if (m.has("previousMonth") && m.has("expensesChange")) {
            BigDecimal difference = m.get("expensesChange").get("difference").decimalValue();
            String previous = monthName(YearMonth.parse(m.get("previousMonth").get("month").asText()));
            if (difference.signum() == 0) {
                summary.append(" Spending was the same as in ").append(previous).append(".");
            } else {
                summary.append(" Compared with ").append(previous).append(", spending was ").append(money(currency, difference.abs()))
                        .append(difference.signum() > 0 ? " higher" : " lower");
                JsonNode percent = m.get("expensesChange").get("percent");
                if (percent != null && !percent.isNull()) {
                    summary.append(" (").append(percent.decimalValue().abs().toPlainString()).append("%)");
                }
                summary.append(".");
            }
        }

        List<String> highlights = new ArrayList<>();
        JsonNode top = m.path("topCategories");
        if (top.isArray() && top.size() > 0) {
            JsonNode first = top.get(0);
            String share = first.has("sharePercent") ? " (" + first.get("sharePercent").decimalValue().toPlainString() + "% of your spending)" : "";
            highlights.add("Your biggest category was " + first.get("name").asText() + " at " + money(currency, first.get("amount").decimalValue()) + share + ".");
        }
        JsonNode merchants = m.path("topMerchants");
        if (merchants.isArray() && merchants.size() > 0) {
            highlights.add("You spent the most with " + merchants.get(0).get("name").asText() + ": " + money(currency, merchants.get(0).get("amount").decimalValue()) + ".");
        }
        for (JsonNode item : m.path("unusual")) {
            String times = item.get("timesTypical").decimalValue().toPlainString();
            if ("LARGE_PAYMENT".equals(item.get("kind").asText())) {
                highlights.add("A single payment, \"" + item.get("label").asText() + "\" on " + formatDate(item.path("date").asText(null)) + ", was "
                        + money(currency, item.get("amount").decimalValue()) + " - about " + times + " times your usual payment of "
                        + money(currency, item.get("typical").decimalValue()) + ".");
            } else {
                highlights.add(item.get("label").asText() + " came to " + money(currency, item.get("amount").decimalValue()) + " - about " + times
                        + " times your usual " + money(currency, item.get("typical").decimalValue()) + ".");
            }
        }
        for (JsonNode budget : m.path("budgets")) {
            String status = budget.get("status").asText();
            if (!"ON_TRACK".equals(status)) {
                String over = budget.has("overBy") ? ", over by " + money(currency, budget.get("overBy").decimalValue()) : "";
                highlights.add("Budget \"" + budget.get("name").asText() + "\": " + money(currency, budget.get("spent").decimalValue()) + " of "
                        + money(currency, budget.get("limit").decimalValue()) + " used (" + budget.get("percentUsed").decimalValue().toPlainString() + "%)" + over + ".");
            }
        }
        return Optional.of(new InsightText(name + " at a glance", summary.toString(), highlights.stream().limit(MAX_HIGHLIGHTS).toList()));
    }

    static String monthName(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }

    private static String formatDate(String iso) {
        if (iso == null) {
            return "that month";
        }
        LocalDate date = LocalDate.parse(iso);
        return date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth();
    }

    static String money(String currency, BigDecimal amount) {
        String symbol = "INR".equals(currency) ? "₹" : currency + " ";
        return symbol + String.format(Locale.US, "%,.2f", amount);
    }
}
