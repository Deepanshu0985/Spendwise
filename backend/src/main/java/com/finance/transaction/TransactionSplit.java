package com.finance.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Editing UI is deferred (ADR-013); the schema and the sum-to-parent invariant (TransactionServiceImpl) exist from V1. */
@Entity
@Table(name = "transaction_splits")
public class TransactionSplit {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TransactionSplit() {
        // JPA
    }

    public TransactionSplit(UUID userId, UUID transactionId, UUID categoryId, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.transactionId = transactionId;
        this.categoryId = categoryId;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
