package com.finance.domain.budget;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class Budget {

    private final UUID id;
    private final UUID userId;
    private String name;
    private BudgetPeriodType periodType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalLimit;
    private final String currency;
    private boolean active;
    private List<BudgetCategoryLimit> categoryLimits;

    public Budget(
            UUID userId, String name, BudgetPeriodType periodType, LocalDate startDate, LocalDate endDate, BigDecimal totalLimit,
            String currency, List<BudgetCategoryLimit> categoryLimits) {
        this(UUID.randomUUID(), userId, name, periodType, startDate, endDate, totalLimit, currency, true, categoryLimits);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public Budget(
            UUID id, UUID userId, String name, BudgetPeriodType periodType, LocalDate startDate, LocalDate endDate,
            BigDecimal totalLimit, String currency, boolean active, List<BudgetCategoryLimit> categoryLimits) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.periodType = periodType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalLimit = totalLimit;
        this.currency = currency;
        this.active = active;
        this.categoryLimits = List.copyOf(categoryLimits);
    }

    public void update(
            String name, BudgetPeriodType periodType, LocalDate startDate, LocalDate endDate, BigDecimal totalLimit,
            List<BudgetCategoryLimit> categoryLimits) {
        this.name = name;
        this.periodType = periodType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalLimit = totalLimit;
        this.categoryLimits = List.copyOf(categoryLimits);
    }

    /** The dates this budget currently measures: the calendar month containing today for MONTHLY, the fixed range otherwise. */
    public LocalDate[] windowAsOf(LocalDate today) {
        if (periodType == BudgetPeriodType.MONTHLY) {
            return new LocalDate[] {today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth())};
        }
        return new LocalDate[] {startDate, endDate};
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

    public BudgetPeriodType getPeriodType() {
        return periodType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getTotalLimit() {
        return totalLimit;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }

    public List<BudgetCategoryLimit> getCategoryLimits() {
        return categoryLimits;
    }
}
