package com.finance.domain.recurring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A stored recurring payment. Detection refreshes its figures; the user's name, category, confirm and dismiss choices are theirs to keep. */
public class RecurringExpense {

    private final UUID id;
    private final UUID userId;
    private final String groupKey;
    private UUID merchantId;
    private UUID categoryId;
    private String name;
    private final String currency;
    private BigDecimal averageAmount;
    private RecurrenceFrequency frequency;
    private LocalDate lastSeenDate;
    private LocalDate nextExpectedDate;
    private BigDecimal monthlyEstimate;
    private BigDecimal yearlyEstimate;
    private int occurrences;
    private BigDecimal confidenceScore;
    private boolean active;
    private boolean confirmed;
    private Instant dismissedAt;

    public RecurringExpense(UUID userId, String name, RecurringCandidate candidate) {
        this(UUID.randomUUID(), userId, candidate.groupKey(), candidate.merchantId(), candidate.categoryId(), name, candidate.currency(),
                null, null, null, null, null, 0, null, true, false, null);
        refreshFrom(candidate);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public RecurringExpense(
            UUID id, UUID userId, String groupKey, UUID merchantId, UUID categoryId, String name, String currency,
            BigDecimal averageAmount, RecurrenceFrequency frequency, LocalDate lastSeenDate, LocalDate nextExpectedDate,
            BigDecimal monthlyEstimate, int occurrences, BigDecimal confidenceScore, boolean active, boolean confirmed,
            Instant dismissedAt) {
        this.id = id;
        this.userId = userId;
        this.groupKey = groupKey;
        this.merchantId = merchantId;
        this.categoryId = categoryId;
        this.name = name;
        this.currency = currency;
        this.averageAmount = averageAmount;
        this.frequency = frequency;
        this.lastSeenDate = lastSeenDate;
        this.nextExpectedDate = nextExpectedDate;
        this.monthlyEstimate = monthlyEstimate;
        this.yearlyEstimate = averageAmount != null && frequency != null ? yearly(averageAmount, frequency) : null;
        this.occurrences = occurrences;
        this.confidenceScore = confidenceScore;
        this.active = active;
        this.confirmed = confirmed;
        this.dismissedAt = dismissedAt;
    }

    /** Updates the detected figures. Name, category, confirmed and dismissed are left alone. */
    public void refreshFrom(RecurringCandidate candidate) {
        this.merchantId = candidate.merchantId() != null ? candidate.merchantId() : this.merchantId;
        if (this.categoryId == null) {
            this.categoryId = candidate.categoryId();
        }
        this.averageAmount = candidate.averageAmount();
        this.frequency = candidate.frequency();
        this.lastSeenDate = candidate.lastSeenDate();
        this.nextExpectedDate = candidate.nextExpectedDate();
        this.yearlyEstimate = yearly(candidate.averageAmount(), candidate.frequency());
        this.monthlyEstimate = this.yearlyEstimate.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        this.occurrences = candidate.occurrences();
        this.confidenceScore = candidate.confidence();
        this.active = candidate.active();
    }

    /** The pattern is no longer found in the user's transactions (for example after they deleted some). */
    public void markNoLongerDetected() {
        this.active = false;
    }

    public void edit(String name, UUID categoryId, Boolean confirmed) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (categoryId != null) {
            this.categoryId = categoryId;
        }
        if (confirmed != null) {
            this.confirmed = confirmed;
        }
    }

    public void dismiss(Instant when) {
        if (this.dismissedAt == null) {
            this.dismissedAt = when;
        }
    }

    public boolean isDismissed() {
        return dismissedAt != null;
    }

    private static BigDecimal yearly(BigDecimal average, RecurrenceFrequency frequency) {
        return average.multiply(BigDecimal.valueOf(frequency.periodsPerYear())).setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }

    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    public LocalDate getLastSeenDate() {
        return lastSeenDate;
    }

    public LocalDate getNextExpectedDate() {
        return nextExpectedDate;
    }

    public BigDecimal getMonthlyEstimate() {
        return monthlyEstimate;
    }

    public BigDecimal getYearlyEstimate() {
        return yearlyEstimate;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Instant getDismissedAt() {
        return dismissedAt;
    }
}
