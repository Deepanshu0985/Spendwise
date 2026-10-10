package com.finance.domain.recurring;

import com.finance.domain.statement.PayeeKey;
import com.finance.domain.transaction.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Finds repeating payments in a user's expense history. Framework-free, deterministic, no model involved: payments to the
 * same payee are grouped, the gaps between them must cluster around one known frequency (a missed payment is tolerated
 * once enough history exists), and the amounts must stay close - tightly for weekly, loosely for monthly or longer so a
 * fluctuating bill still counts. Ordinary habits (coffee, groceries) fail the amount or gap checks and are left out.
 */
public final class RecurrenceDetector {

    static final int MIN_OCCURRENCES = 3;
    static final int MIN_YEARLY_OCCURRENCES = 2;
    static final double MIN_REGULAR_GAP_SHARE = 0.75;
    static final double FIXED_AMOUNT_TOLERANCE = 0.10;
    static final double VARIABLE_AMOUNT_TOLERANCE = 0.50;
    static final double MIN_CONFIDENCE = 0.60;

    private static final Pattern LONG_NUMBER = Pattern.compile("\\b\\d{4,}\\b");

    private RecurrenceDetector() {
    }

    /** expenses: confirmed EXPENSE transactions in any order. today: decides whether a pattern is still running. */
    public static List<RecurringCandidate> detect(List<Transaction> expenses, LocalDate today) {
        Map<String, List<Transaction>> groups = new LinkedHashMap<>();
        for (Transaction transaction : expenses) {
            groups.computeIfAbsent(groupKey(transaction), key -> new ArrayList<>()).add(transaction);
        }
        List<RecurringCandidate> found = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> group : groups.entrySet()) {
            RecurringCandidate candidate = analyse(group.getKey(), group.getValue(), today);
            if (candidate != null) {
                found.add(candidate);
            }
        }
        return found;
    }

    /** Payee identity: the merchant when one is assigned, otherwise the description with reference-like numbers removed. */
    public static String groupKey(Transaction transaction) {
        String currency = transaction.getCurrency();
        if (transaction.getMerchantId() != null) {
            return "merchant:" + transaction.getMerchantId() + ":" + currency;
        }
        String text = transaction.getDescription() != null ? transaction.getDescription() : transaction.getRawDescription();
        String key = PayeeKey.of(LONG_NUMBER.matcher(text == null ? "" : text).replaceAll(" "));
        return "text:" + key + ":" + currency;
    }

    private static RecurringCandidate analyse(String key, List<Transaction> transactions, LocalDate today) {
        if (key.startsWith("text::")) {
            return null; // no usable description to group by
        }
        // One occurrence per day: two charges on the same date are one payment of their combined amount.
        Map<LocalDate, List<Transaction>> byDate = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionDate));
        List<LocalDate> dates = byDate.keySet().stream().sorted().toList();
        if (dates.size() < MIN_YEARLY_OCCURRENCES) {
            return null;
        }
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < dates.size(); i++) {
            gaps.add(ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i)));
        }
        RecurrenceFrequency frequency = RecurrenceFrequency.fromMedianGap(median(gaps));
        if (frequency == null) {
            return null;
        }
        if (dates.size() < (frequency == RecurrenceFrequency.YEARLY ? MIN_YEARLY_OCCURRENCES : MIN_OCCURRENCES)) {
            return null;
        }
        double regularShare = (double) gaps.stream().filter(frequency::acceptsGap).count() / gaps.size();
        if (regularShare < MIN_REGULAR_GAP_SHARE) {
            return null;
        }

        List<BigDecimal> amounts = dates.stream()
                .map(date -> byDate.get(date).stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .toList();
        BigDecimal medianAmount = medianAmount(amounts);
        double spread = amounts.stream()
                .mapToDouble(amount -> amount.subtract(medianAmount).abs().doubleValue() / medianAmount.doubleValue())
                .max().orElse(0);
        double allowed = frequency == RecurrenceFrequency.WEEKLY ? FIXED_AMOUNT_TOLERANCE : VARIABLE_AMOUNT_TOLERANCE;
        if (medianAmount.signum() <= 0 || spread > allowed) {
            return null;
        }

        double amountScore = Math.max(0, 1 - spread / VARIABLE_AMOUNT_TOLERANCE);
        double countScore = Math.min(1, (dates.size() - 2) / 4.0);
        double confidence = 0.4 * regularShare + 0.3 * amountScore + 0.3 * countScore;
        if (confidence < MIN_CONFIDENCE) {
            return null;
        }

        LocalDate lastSeen = dates.get(dates.size() - 1);
        LocalDate next = frequency.nextAfter(lastSeen);
        boolean active = !next.plusDays(frequency.graceDays()).isBefore(today);
        List<BigDecimal> recent = amounts.subList(Math.max(0, amounts.size() - 3), amounts.size());
        BigDecimal average = recent.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(recent.size()), 2, RoundingMode.HALF_UP);

        Transaction latest = byDate.get(lastSeen).get(0);
        return new RecurringCandidate(
                key, mostCommon(transactions.stream().map(Transaction::getMerchantId).toList()),
                mostCommon(transactions.stream().map(Transaction::getCategoryId).toList()),
                latest.getDescription() != null ? latest.getDescription() : latest.getRawDescription(),
                latest.getCurrency(), average, frequency, lastSeen, next, active, dates.size(),
                BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP));
    }

    private static long median(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        return sorted.get(sorted.size() / 2);
    }

    private static BigDecimal medianAmount(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        return sorted.get(sorted.size() / 2);
    }

    private static UUID mostCommon(List<UUID> ids) {
        Map<UUID, Integer> counts = new HashMap<>();
        ids.stream().filter(id -> id != null).forEach(id -> counts.merge(id, 1, Integer::sum));
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }
}
