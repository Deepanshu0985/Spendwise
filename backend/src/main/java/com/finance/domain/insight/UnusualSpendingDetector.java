package com.finance.domain.insight;

import com.finance.domain.statement.PayeeKey;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Finds spending that stands out against the user's own history, with two plain rules and no model:
 * a category whose month is well above what it usually is, and a single payment far above their normal payments. Both are
 * judged against the user's own scale, so the same rules suit a small and a large budget. Framework-free and deterministic.
 */
public final class UnusualSpendingDetector {

    static final BigDecimal SPIKE_RATIO = new BigDecimal("1.5");
    static final int MIN_HISTORY_MONTHS = 2;
    /** A rise must also matter: at least this share of the user's usual monthly spending. */
    static final BigDecimal MIN_INCREASE_SHARE = new BigDecimal("0.05");
    static final BigDecimal LARGE_PAYMENT_MULTIPLE = new BigDecimal("3");
    static final int MIN_PAYMENT_HISTORY = 10;
    /** A payment is familiar if the user has made this many earlier payments of about the same amount. */
    static final int FAMILIAR_AMOUNT_REPEATS = 2;
    static final BigDecimal FAMILIAR_AMOUNT_TOLERANCE = new BigDecimal("0.05");
    static final int MAX_PAYMENTS_REPORTED = 3;
    static final int MAX_ITEMS = 5;

    public enum Kind {
        CATEGORY_SPIKE,
        LARGE_PAYMENT
    }

    public record Payment(String description, LocalDate date, BigDecimal amount) {
    }

    /** label is the category name or the payment's description; date is set only for a payment. */
    public record Unusual(Kind kind, String label, BigDecimal amount, BigDecimal typical, BigDecimal increase, BigDecimal timesTypical, LocalDate date) {
    }

    private UnusualSpendingDetector() {
    }

    /**
     * @param currentCategories       category name to this month's spending
     * @param previousMonths          the same for each of the months before it that had any spending (typically up to three)
     * @param averageMonthlyExpenses  the user's usual total spending per month, from those earlier months
     * @param thisMonthPayments       the month's individual expense payments
     * @param historicalPayments      the user's individual expense payments in the months before; a large payment that matches one of
     *                                them (same description, or the same amount again) is a regular bill, not an unusual payment
     */
    public static List<Unusual> detect(
            Map<String, BigDecimal> currentCategories, List<Map<String, BigDecimal>> previousMonths, BigDecimal averageMonthlyExpenses,
            List<Payment> thisMonthPayments, List<Payment> historicalPayments) {
        if (averageMonthlyExpenses == null || averageMonthlyExpenses.signum() <= 0) {
            return List.of();
        }
        BigDecimal minimumRise = averageMonthlyExpenses.multiply(MIN_INCREASE_SHARE);
        List<Unusual> found = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : currentCategories.entrySet()) {
            List<BigDecimal> earlier = previousMonths.stream()
                    .map(month -> month.get(entry.getKey())).filter(amount -> amount != null && amount.signum() > 0).toList();
            if (earlier.size() < MIN_HISTORY_MONTHS) {
                continue;
            }
            BigDecimal typical = earlier.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(earlier.size()), 2, RoundingMode.HALF_UP);
            BigDecimal rise = entry.getValue().subtract(typical);
            if (typical.signum() > 0 && entry.getValue().compareTo(typical.multiply(SPIKE_RATIO)) >= 0 && rise.compareTo(minimumRise) >= 0) {
                found.add(new Unusual(Kind.CATEGORY_SPIKE, entry.getKey(), money(entry.getValue()), typical, money(rise), times(entry.getValue(), typical), null));
            }
        }

        if (historicalPayments.size() >= MIN_PAYMENT_HISTORY) {
            BigDecimal median = median(historicalPayments.stream().map(Payment::amount).toList());
            BigDecimal threshold = median.multiply(LARGE_PAYMENT_MULTIPLE);
            thisMonthPayments.stream()
                    .filter(p -> median.signum() > 0 && p.amount().compareTo(threshold) >= 0 && p.amount().compareTo(minimumRise) >= 0
                            && !isFamiliar(p, historicalPayments))
                    .sorted(Comparator.comparing(Payment::amount).reversed())
                    .limit(MAX_PAYMENTS_REPORTED)
                    .forEach(p -> found.add(new Unusual(
                            Kind.LARGE_PAYMENT, p.description(), money(p.amount()), money(median), money(p.amount().subtract(median)), times(p.amount(), median), p.date())));
        }

        return found.stream().sorted(Comparator.comparing(Unusual::increase).reversed()).limit(MAX_ITEMS).toList();
    }

    /** Rent, a loan instalment or a regular order: the same description again, or the same amount several times before. */
    private static boolean isFamiliar(Payment payment, List<Payment> history) {
        String key = descriptionKey(payment.description());
        if (!key.isEmpty() && history.stream().anyMatch(h -> descriptionKey(h.description()).equals(key))) {
            return true;
        }
        BigDecimal margin = payment.amount().multiply(FAMILIAR_AMOUNT_TOLERANCE);
        long similar = history.stream().filter(h -> h.amount().subtract(payment.amount()).abs().compareTo(margin) <= 0).count();
        return similar >= FAMILIAR_AMOUNT_REPEATS;
    }

    /** The description reduced to its words with numbers dropped, so "Cab ride 3" and "Cab ride 4" are the same payee. */
    private static String descriptionKey(String description) {
        return PayeeKey.of(description).replaceAll("\\d+", "").replaceAll("\\s+", " ").trim();
    }

    static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int mid = sorted.size() / 2;
        return money(sorted.size() % 2 == 1 ? sorted.get(mid) : sorted.get(mid - 1).add(sorted.get(mid)).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal times(BigDecimal amount, BigDecimal typical) {
        return amount.divide(typical, 1, RoundingMode.HALF_UP);
    }
}
