package com.finance.infrastructure.persistence.statement;

import com.finance.domain.statement.DuplicateReason;
import com.finance.domain.statement.DuplicateStatus;
import com.finance.domain.statement.ReviewStatus;
import com.finance.domain.transaction.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "statement_transactions")
public class StatementTransactionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "statement_id", nullable = false)
    private UUID statementId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "raw_description", nullable = false)
    private String rawDescription;

    @Column(name = "normalized_description")
    private String normalizedDescription;

    @Column(name = "suggested_merchant_id")
    private UUID suggestedMerchantId;

    @Column(name = "suggested_category_id")
    private UUID suggestedCategoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_transaction_type", nullable = false)
    private TransactionType suggestedTransactionType;

    @Column(name = "confidence_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "duplicate_status", nullable = false)
    private DuplicateStatus duplicateStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus;

    @Column(name = "canonical_transaction_id")
    private UUID canonicalTransactionId;

    @Column(name = "external_reference")
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "duplicate_reason")
    private DuplicateReason duplicateReason;

    @Column(name = "duplicate_of_transaction_id")
    private UUID duplicateOfTransactionId;

    @Column(name = "duplicate_overridden_at")
    private Instant duplicateOverriddenAt;

    @Column(name = "source_row_reference", nullable = false)
    private String sourceRowReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StatementTransactionJpaEntity() {
        // JPA
    }

    StatementTransactionJpaEntity(
            UUID id,
            UUID userId,
            UUID statementId,
            LocalDate transactionDate,
            BigDecimal amount,
            String currency,
            String rawDescription,
            String normalizedDescription,
            UUID suggestedMerchantId,
            UUID suggestedCategoryId,
            TransactionType suggestedTransactionType,
            BigDecimal confidenceScore,
            DuplicateStatus duplicateStatus,
            ReviewStatus reviewStatus,
            UUID canonicalTransactionId,
            String sourceRowReference,
            String externalReference,
            DuplicateReason duplicateReason,
            UUID duplicateOfTransactionId,
            Instant duplicateOverriddenAt) {
        this.id = id;
        this.userId = userId;
        this.statementId = statementId;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.currency = currency;
        this.rawDescription = rawDescription;
        this.normalizedDescription = normalizedDescription;
        this.suggestedMerchantId = suggestedMerchantId;
        this.suggestedCategoryId = suggestedCategoryId;
        this.suggestedTransactionType = suggestedTransactionType;
        this.confidenceScore = confidenceScore;
        this.duplicateStatus = duplicateStatus;
        this.reviewStatus = reviewStatus;
        this.canonicalTransactionId = canonicalTransactionId;
        this.sourceRowReference = sourceRowReference;
        this.externalReference = externalReference;
        this.duplicateReason = duplicateReason;
        this.duplicateOfTransactionId = duplicateOfTransactionId;
        this.duplicateOverriddenAt = duplicateOverriddenAt;
    }

    String getExternalReference() {
        return externalReference;
    }

    DuplicateReason getDuplicateReason() {
        return duplicateReason;
    }

    void setDuplicateReason(DuplicateReason duplicateReason) {
        this.duplicateReason = duplicateReason;
    }

    UUID getDuplicateOfTransactionId() {
        return duplicateOfTransactionId;
    }

    void setDuplicateOfTransactionId(UUID duplicateOfTransactionId) {
        this.duplicateOfTransactionId = duplicateOfTransactionId;
    }

    Instant getDuplicateOverriddenAt() {
        return duplicateOverriddenAt;
    }

    void setDuplicateOverriddenAt(Instant duplicateOverriddenAt) {
        this.duplicateOverriddenAt = duplicateOverriddenAt;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getStatementId() {
        return statementId;
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

    String getRawDescription() {
        return rawDescription;
    }

    String getNormalizedDescription() {
        return normalizedDescription;
    }

    UUID getSuggestedMerchantId() {
        return suggestedMerchantId;
    }

    void setSuggestedMerchantId(UUID suggestedMerchantId) {
        this.suggestedMerchantId = suggestedMerchantId;
    }

    UUID getSuggestedCategoryId() {
        return suggestedCategoryId;
    }

    void setSuggestedCategoryId(UUID suggestedCategoryId) {
        this.suggestedCategoryId = suggestedCategoryId;
    }

    TransactionType getSuggestedTransactionType() {
        return suggestedTransactionType;
    }

    void setSuggestedTransactionType(TransactionType suggestedTransactionType) {
        this.suggestedTransactionType = suggestedTransactionType;
    }

    BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    void setDuplicateStatus(DuplicateStatus duplicateStatus) {
        this.duplicateStatus = duplicateStatus;
    }

    DuplicateStatus getDuplicateStatus() {
        return duplicateStatus;
    }

    ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    UUID getCanonicalTransactionId() {
        return canonicalTransactionId;
    }

    void setCanonicalTransactionId(UUID canonicalTransactionId) {
        this.canonicalTransactionId = canonicalTransactionId;
    }

    String getSourceRowReference() {
        return sourceRowReference;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
