package com.finance.infrastructure.persistence.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_splits")
public class TransactionSplitJpaEntity {

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

    protected TransactionSplitJpaEntity() {
        // JPA
    }

    TransactionSplitJpaEntity(UUID id, UUID userId, UUID transactionId, UUID categoryId, BigDecimal amount) {
        this.id = id;
        this.userId = userId;
        this.transactionId = transactionId;
        this.categoryId = categoryId;
        this.amount = amount;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getTransactionId() {
        return transactionId;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
