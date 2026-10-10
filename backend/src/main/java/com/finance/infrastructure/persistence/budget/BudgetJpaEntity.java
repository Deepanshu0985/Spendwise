package com.finance.infrastructure.persistence.budget;

import com.finance.domain.budget.BudgetPeriodType;
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
@Table(name = "budgets")
public class BudgetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private BudgetPeriodType periodType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_limit", nullable = false)
    private BigDecimal totalLimit;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BudgetJpaEntity() {
        // JPA
    }

    BudgetJpaEntity(UUID id, UUID userId, String currency) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    String getName() { return name; }
    BudgetPeriodType getPeriodType() { return periodType; }
    LocalDate getStartDate() { return startDate; }
    LocalDate getEndDate() { return endDate; }
    BigDecimal getTotalLimit() { return totalLimit; }
    String getCurrency() { return currency; }
    boolean isActive() { return active; }

    void setName(String name) { this.name = name; }
    void setPeriodType(BudgetPeriodType periodType) { this.periodType = periodType; }
    void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    void setTotalLimit(BigDecimal totalLimit) { this.totalLimit = totalLimit; }
    void setActive(boolean active) { this.active = active; }
}
