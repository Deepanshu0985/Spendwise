package com.finance.domain.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Goal {

    private final UUID id;
    private final UUID userId;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private final String currency;
    private GoalStatus status;

    public Goal(UUID userId, String name, BigDecimal targetAmount, BigDecimal currentAmount, LocalDate targetDate, String currency) {
        this(UUID.randomUUID(), userId, name, targetAmount, currentAmount, targetDate, currency, GoalStatus.ACTIVE);
        refreshStatus();
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public Goal(
            UUID id, UUID userId, String name, BigDecimal targetAmount, BigDecimal currentAmount, LocalDate targetDate,
            String currency, GoalStatus status) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.targetDate = targetDate;
        this.currency = currency;
        this.status = status;
    }

    public void update(String name, BigDecimal targetAmount, BigDecimal currentAmount, LocalDate targetDate) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.targetDate = targetDate;
        refreshStatus();
    }

    /** A goal is achieved once the saved amount reaches the target, and active again if either figure is later corrected. */
    private void refreshStatus() {
        this.status = currentAmount.compareTo(targetAmount) >= 0 ? GoalStatus.ACHIEVED : GoalStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public String getCurrency() {
        return currency;
    }

    public GoalStatus getStatus() {
        return status;
    }
}
