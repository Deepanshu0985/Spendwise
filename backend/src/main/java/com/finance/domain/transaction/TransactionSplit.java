package com.finance.domain.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Editing UI is deferred (ADR-013); the schema and the sum-to-parent invariant (TransactionServiceImpl) exist from V1. */
public class TransactionSplit {

    private final UUID id;
    private final UUID userId;
    private final UUID transactionId;
    private final UUID categoryId;
    private final BigDecimal amount;
    private final Instant createdAt;

    public TransactionSplit(UUID userId, UUID transactionId, UUID categoryId, BigDecimal amount) {
        this(UUID.randomUUID(), userId, transactionId, categoryId, amount, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public TransactionSplit(UUID id, UUID userId, UUID transactionId, UUID categoryId, BigDecimal amount, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.transactionId = transactionId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.createdAt = createdAt;
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
