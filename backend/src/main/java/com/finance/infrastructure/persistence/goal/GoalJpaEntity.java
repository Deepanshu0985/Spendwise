package com.finance.infrastructure.persistence.goal;

import com.finance.domain.goal.GoalStatus;
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
@Table(name = "goals")
public class GoalJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_amount", nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false)
    private BigDecimal currentAmount;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GoalJpaEntity() {
        // JPA
    }

    GoalJpaEntity(UUID id, UUID userId, String currency) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    String getName() { return name; }
    BigDecimal getTargetAmount() { return targetAmount; }
    BigDecimal getCurrentAmount() { return currentAmount; }
    LocalDate getTargetDate() { return targetDate; }
    String getCurrency() { return currency; }
    GoalStatus getStatus() { return status; }

    void setName(String name) { this.name = name; }
    void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    void setStatus(GoalStatus status) { this.status = status; }
}
