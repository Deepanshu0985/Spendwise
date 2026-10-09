package com.finance.infrastructure.persistence.recurring;

import com.finance.domain.recurring.RecurrenceFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recurring_expenses")
public class RecurringExpenseJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "group_key", nullable = false)
    private String groupKey;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "average_amount", nullable = false)
    private BigDecimal averageAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceFrequency frequency;

    @Column(name = "last_seen_date", nullable = false)
    private LocalDate lastSeenDate;

    @Column(name = "next_expected_date", nullable = false)
    private LocalDate nextExpectedDate;

    @Column(name = "monthly_estimate", nullable = false)
    private BigDecimal monthlyEstimate;

    @Column(name = "yearly_estimate", nullable = false)
    private BigDecimal yearlyEstimate;

    @Column(nullable = false)
    private int occurrences;

    @Column(name = "confidence_score", nullable = false)
    private BigDecimal confidenceScore;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean confirmed;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecurringExpenseJpaEntity() {
        // JPA
    }

    RecurringExpenseJpaEntity(UUID id, UUID userId, String groupKey, String currency) {
        this.id = id;
        this.userId = userId;
        this.groupKey = groupKey;
        this.currency = currency;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    String getGroupKey() { return groupKey; }
    UUID getMerchantId() { return merchantId; }
    UUID getCategoryId() { return categoryId; }
    String getName() { return name; }
    String getCurrency() { return currency; }
    BigDecimal getAverageAmount() { return averageAmount; }
    RecurrenceFrequency getFrequency() { return frequency; }
    LocalDate getLastSeenDate() { return lastSeenDate; }
    LocalDate getNextExpectedDate() { return nextExpectedDate; }
    BigDecimal getMonthlyEstimate() { return monthlyEstimate; }
    BigDecimal getYearlyEstimate() { return yearlyEstimate; }
    int getOccurrences() { return occurrences; }
    BigDecimal getConfidenceScore() { return confidenceScore; }
    boolean isActive() { return active; }
    boolean isConfirmed() { return confirmed; }
    Instant getDismissedAt() { return dismissedAt; }

    void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    void setName(String name) { this.name = name; }
    void setAverageAmount(BigDecimal averageAmount) { this.averageAmount = averageAmount; }
    void setFrequency(RecurrenceFrequency frequency) { this.frequency = frequency; }
    void setLastSeenDate(LocalDate lastSeenDate) { this.lastSeenDate = lastSeenDate; }
    void setNextExpectedDate(LocalDate nextExpectedDate) { this.nextExpectedDate = nextExpectedDate; }
    void setMonthlyEstimate(BigDecimal monthlyEstimate) { this.monthlyEstimate = monthlyEstimate; }
    void setYearlyEstimate(BigDecimal yearlyEstimate) { this.yearlyEstimate = yearlyEstimate; }
    void setOccurrences(int occurrences) { this.occurrences = occurrences; }
    void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    void setActive(boolean active) { this.active = active; }
    void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    void setDismissedAt(Instant dismissedAt) { this.dismissedAt = dismissedAt; }
}
