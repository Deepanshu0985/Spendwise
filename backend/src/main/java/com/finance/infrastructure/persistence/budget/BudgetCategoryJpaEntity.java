package com.finance.infrastructure.persistence.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_categories")
public class BudgetCategoryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "limit_amount", nullable = false)
    private BigDecimal limitAmount;

    protected BudgetCategoryJpaEntity() {
        // JPA
    }

    BudgetCategoryJpaEntity(UUID userId, UUID budgetId, UUID categoryId, BigDecimal limitAmount) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.limitAmount = limitAmount;
    }

    UUID getBudgetId() {
        return budgetId;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    BigDecimal getLimitAmount() {
        return limitAmount;
    }

}
