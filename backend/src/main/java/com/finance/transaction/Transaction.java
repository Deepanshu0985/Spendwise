package com.finance.transaction;

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

/**
 * transaction_date is a calendar date (LocalDate), never an instant - no
 * timezone conversion is ever applied to it (analytics-specification.md).
 * amount is always a positive magnitude; transactionType supplies the ledger
 * side (ADR-012) - see TransactionType.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column
    private String description;

    @Column(name = "raw_description")
    private String rawDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionSource source;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(name = "transfer_group_id")
    private UUID transferGroupId;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
        // JPA
    }

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
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.accountId = accountId;
        this.merchantId = merchantId;
        this.categoryId = categoryId;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.transactionType = transactionType;
        this.source = source;
        this.status = status;
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
