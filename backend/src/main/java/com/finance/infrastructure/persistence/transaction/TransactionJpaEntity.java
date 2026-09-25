package com.finance.infrastructure.persistence.transaction;

import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;
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
@Table(name = "transactions")
public class TransactionJpaEntity {

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

    protected TransactionJpaEntity() {
        // JPA
    }

    TransactionJpaEntity(
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
            TransactionStatus status) {
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
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getAccountId() {
        return accountId;
    }

    void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    UUID getMerchantId() {
        return merchantId;
    }

    void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    LocalDate getTransactionDate() {
        return transactionDate;
    }

    void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    BigDecimal getAmount() {
        return amount;
    }

    void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    String getCurrency() {
        return currency;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    String getRawDescription() {
        return rawDescription;
    }

    TransactionType getTransactionType() {
        return transactionType;
    }

    String getPaymentMethod() {
        return paymentMethod;
    }

    TransactionSource getSource() {
        return source;
    }

    String getSourceReference() {
        return sourceReference;
    }

    String getExternalTransactionId() {
        return externalTransactionId;
    }

    UUID getTransferGroupId() {
        return transferGroupId;
    }

    void setTransferGroupId(UUID transferGroupId) {
        this.transferGroupId = transferGroupId;
    }

    BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    TransactionStatus getStatus() {
        return status;
    }

    void setStatus(TransactionStatus status) {
        this.status = status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
