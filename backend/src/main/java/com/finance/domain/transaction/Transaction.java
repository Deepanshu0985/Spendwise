package com.finance.domain.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pure business model - no persistence framework dependency; see
 * infrastructure.persistence.transaction for the JPA mapping. transaction_date
 * is a calendar date (LocalDate), never an instant - no timezone conversion is
 * ever applied to it (analytics-specification.md). amount is always a positive
 * magnitude; transactionType supplies the ledger side (ADR-012).
 */
public class Transaction {

    private final UUID id;
    private final UUID userId;
    private UUID accountId;
    private UUID merchantId;
    private UUID categoryId;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private final String currency;
    private String description;
    private final String rawDescription;
    private final TransactionType transactionType;
    private final String paymentMethod;
    private final TransactionSource source;
    private final String sourceReference;
    private final String externalTransactionId;
    private UUID transferGroupId;
    private final BigDecimal confidenceScore;
    private TransactionStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Transaction(
            UUID userId,
            UUID accountId,
            UUID merchantId,
            UUID categoryId,
            LocalDate transactionDate,
            BigDecimal amount,
            String currency,
            String description,
            TransactionType transactionType,
            TransactionSource source,
            TransactionStatus status) {
        this(
                UUID.randomUUID(), userId, accountId, merchantId, categoryId, transactionDate, amount, currency, description,
                null, transactionType, null, source, null, null, null, null, status, null, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public Transaction(
            UUID id,
            UUID userId,
            UUID accountId,
            UUID merchantId,
            UUID categoryId,
            LocalDate transactionDate,
            BigDecimal amount,
            String currency,
            String description,
            String rawDescription,
            TransactionType transactionType,
            String paymentMethod,
            TransactionSource source,
            String sourceReference,
            String externalTransactionId,
            UUID transferGroupId,
            BigDecimal confidenceScore,
            TransactionStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.accountId = accountId;
        this.merchantId = merchantId;
        this.categoryId = categoryId;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.rawDescription = rawDescription;
        this.transactionType = transactionType;
        this.paymentMethod = paymentMethod;
        this.source = source;
        this.sourceReference = sourceReference;
        this.externalTransactionId = externalTransactionId;
        this.transferGroupId = transferGroupId;
        this.confidenceScore = confidenceScore;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(UUID accountId, UUID merchantId, UUID categoryId, LocalDate transactionDate, BigDecimal amount, String description) {
        this.accountId = accountId;
        this.merchantId = merchantId;
        this.categoryId = categoryId;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.description = description;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public void setTransferGroupId(UUID transferGroupId) {
        this.transferGroupId = transferGroupId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getRawDescription() {
        return rawDescription;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public TransactionSource getSource() {
        return source;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public UUID getTransferGroupId() {
        return transferGroupId;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
