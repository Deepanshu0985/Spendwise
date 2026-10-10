package com.finance.domain.recurring;

import java.time.LocalDate;

/** How often a recurring payment repeats, with the day-gap band each frequency accepts and how long a missed one is tolerated. */
public enum RecurrenceFrequency {
    WEEKLY(6, 8, 52, 14),
    MONTHLY(26, 35, 12, 15),
    QUARTERLY(84, 98, 4, 30),
    YEARLY(350, 380, 1, 45);

    private final int minGapDays;
    private final int maxGapDays;
    private final int periodsPerYear;
    private final int graceDays;

    RecurrenceFrequency(int minGapDays, int maxGapDays, int periodsPerYear, int graceDays) {
        this.minGapDays = minGapDays;
        this.maxGapDays = maxGapDays;
        this.periodsPerYear = periodsPerYear;
        this.graceDays = graceDays;
    }

    public boolean acceptsGap(long days) {
        return days >= minGapDays && days <= maxGapDays;
    }

    public int periodsPerYear() {
        return periodsPerYear;
    }

    /** Days past the expected date after which the payment is considered to have stopped. */
    public int graceDays() {
        return graceDays;
    }

    /** The date one period after the given one; monthly-style periods keep the day of month (clamped to short months). */
    public LocalDate nextAfter(LocalDate date) {
        return switch (this) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case YEARLY -> date.plusYears(1);
        };
    }

    /** Which frequency a typical gap between payments points to, or null when it fits none. */
    public static RecurrenceFrequency fromMedianGap(long medianDays) {
        for (RecurrenceFrequency frequency : values()) {
            if (frequency.acceptsGap(medianDays)) {
                return frequency;
            }
        }
        return null;
    }
}
