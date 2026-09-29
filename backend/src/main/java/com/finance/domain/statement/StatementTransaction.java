package com.finance.domain.statement;

import com.finance.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single staged, not-yet-imported row from a parsed statement. Pure business
 * model - no persistence framework dependency; see
 * infrastructure.persistence.statement for the JPA mapping.
 */
public class StatementTransaction {

    private final UUID id;
    private final UUID userId;
    private final UUID statementId;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private final String currency;
    private final String rawDescription;
    private String normalizedDescription;
    private UUID suggestedMerchantId;
    private UUID suggestedCategoryId;
    private TransactionType suggestedTransactionType;
    private final BigDecimal confidenceScore;
    private DuplicateStatus duplicateStatus;
    private ReviewStatus reviewStatus;
    private UUID canonicalTransactionId;
    private final String sourceRowReference;
    private final Instant createdAt;

    public StatementTransaction(
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
            String sourceRowReference) {
        this(
                UUID.randomUUID(), userId, statementId, transactionDate, amount, currency, rawDescription, normalizedDescription,
                suggestedMerchantId, suggestedCategoryId, suggestedTransactionType, confidenceScore, DuplicateStatus.UNKNOWN,
                ReviewStatus.PENDING, null, sourceRowReference, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public StatementTransaction(
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
            Instant createdAt) {
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
        this.createdAt = createdAt;
    }

    /** The user correcting a staged row before confirming - moves review status to EDITED. */
    public void applyReview(
            LocalDate transactionDate, BigDecimal amount, UUID suggestedMerchantId, UUID suggestedCategoryId,
            TransactionType suggestedTransactionType) {
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.suggestedMerchantId = suggestedMerchantId;
        this.suggestedCategoryId = suggestedCategoryId;
        this.suggestedTransactionType = suggestedTransactionType;
        this.reviewStatus = ReviewStatus.EDITED;
    }

    public void markPromoted(UUID canonicalTransactionId) {
        this.canonicalTransactionId = canonicalTransactionId;
        this.reviewStatus = ReviewStatus.ACCEPTED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getStatementId() {
        return statementId;
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

    public String getRawDescription() {
        return rawDescription;
    }

    public String getNormalizedDescription() {
        return normalizedDescription;
    }

    public UUID getSuggestedMerchantId() {
        return suggestedMerchantId;
    }

    public UUID getSuggestedCategoryId() {
        return suggestedCategoryId;
    }

    public TransactionType getSuggestedTransactionType() {
        return suggestedTransactionType;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public DuplicateStatus getDuplicateStatus() {
        return duplicateStatus;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public UUID getCanonicalTransactionId() {
        return canonicalTransactionId;
    }

    public String getSourceRowReference() {
        return sourceRowReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
