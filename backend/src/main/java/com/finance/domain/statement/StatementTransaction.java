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
    private final String externalReference;
    private DuplicateReason duplicateReason;
    private UUID duplicateOfTransactionId;
    private Instant duplicateOverriddenAt;
    private final String sourceAccountLabel;
    private UUID accountId;
    private boolean aiSuggested;
    private String aiReason;

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
            String sourceRowReference,
            String externalReference) {
        this(
                userId, statementId, transactionDate, amount, currency, rawDescription, normalizedDescription, suggestedMerchantId,
                suggestedCategoryId, suggestedTransactionType, confidenceScore, sourceRowReference, externalReference, null);
    }

    /** sourceAccountLabel: the account the statement says this payment came from (null when the statement does not say). */
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
            String sourceRowReference,
            String externalReference,
            String sourceAccountLabel) {
        this(
                UUID.randomUUID(), userId, statementId, transactionDate, amount, currency, rawDescription, normalizedDescription,
                suggestedMerchantId, suggestedCategoryId, suggestedTransactionType, confidenceScore, DuplicateStatus.UNKNOWN,
                ReviewStatus.PENDING, null, sourceRowReference, null, externalReference, null, null, null, sourceAccountLabel, null, false, null);
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
            Instant createdAt,
            String externalReference,
            DuplicateReason duplicateReason,
            UUID duplicateOfTransactionId,
            Instant duplicateOverriddenAt,
            String sourceAccountLabel,
            UUID accountId,
            boolean aiSuggested,
            String aiReason) {
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
        this.externalReference = externalReference;
        this.duplicateReason = duplicateReason;
        this.duplicateOfTransactionId = duplicateOfTransactionId;
        this.duplicateOverriddenAt = duplicateOverriddenAt;
        this.sourceAccountLabel = sourceAccountLabel;
        this.accountId = accountId;
        this.aiSuggested = aiSuggested;
        this.aiReason = aiReason;
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
        // The user has now looked at the row and decided, so it is no longer "the model's suggestion".
        this.aiSuggested = false;
        this.aiReason = null;
    }

    /**
     * Applies a model's suggestion, but only to a row nobody has reviewed and that has no category yet. The type is
     * taken only when the row's own type is still unknown - the statement's own signals outrank a guess.
     */
    public boolean applyAiSuggestion(UUID categoryId, TransactionType type, String reason) {
        if (reviewStatus != ReviewStatus.PENDING || suggestedCategoryId != null || categoryId == null) {
            return false;
        }
        this.suggestedCategoryId = categoryId;
        if (suggestedTransactionType == null || suggestedTransactionType.isUnknown()) {
            this.suggestedTransactionType = type;
        }
        this.aiSuggested = true;
        this.aiReason = reason;
        return true;
    }

    /** Fills merchant/category from a remembered rule, but only where the row has none and the user hasn't reviewed it. */
    public boolean applySuggestion(UUID merchantId, UUID categoryId) {
        if (reviewStatus != ReviewStatus.PENDING) {
            return false;
        }
        boolean changed = false;
        if (suggestedMerchantId == null && merchantId != null) {
            this.suggestedMerchantId = merchantId;
            changed = true;
        }
        if (suggestedCategoryId == null && categoryId != null) {
            this.suggestedCategoryId = categoryId;
            changed = true;
        }
        return changed;
    }

    /** Records the outcome of duplicate scoring; never touches a row whose duplicate flag the user already overrode. */
    public void applyDuplicateScore(DuplicateMatch match) {
        if (duplicateOverriddenAt != null) {
            return;
        }
        if (match == null) {
            this.duplicateStatus = DuplicateStatus.NOT_DUPLICATE;
            this.duplicateReason = null;
            this.duplicateOfTransactionId = null;
            return;
        }
        this.duplicateStatus = match.status();
        this.duplicateReason = match.reason();
        this.duplicateOfTransactionId = match.matchedTransactionId();
    }

    /** The user choosing to import a flagged row anyway - kept as audit metadata so later rescoring can't undo it. */
    public void overrideDuplicate(Instant at) {
        this.duplicateStatus = DuplicateStatus.NOT_DUPLICATE;
        this.duplicateOverriddenAt = at;
    }

    /** The user choosing not to import this row at all (kept in the review list, skipped on confirm). */
    public void skip() {
        this.reviewStatus = ReviewStatus.REJECTED;
    }

    /** Undoes skip(): the row goes back to being imported on confirm. */
    public void restore() {
        this.reviewStatus = ReviewStatus.PENDING;
    }

    public boolean isUnresolvedDuplicate() {
        return duplicateStatus == DuplicateStatus.DUPLICATE && duplicateOverriddenAt == null;
    }

    /** The user (or a remembered rule) saying which of their accounts this payment really came from. */
    public void assignAccount(UUID accountId) {
        this.accountId = accountId;
    }

    /** The account this row belongs to: its own assignment if it has one, otherwise the statement's account. */
    public UUID effectiveAccountId(UUID statementAccountId) {
        return accountId != null ? accountId : statementAccountId;
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

    public String getSourceAccountLabel() {
        return sourceAccountLabel;
    }

    public boolean isAiSuggested() {
        return aiSuggested;
    }

    public String getAiReason() {
        return aiReason;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public DuplicateReason getDuplicateReason() {
        return duplicateReason;
    }

    public UUID getDuplicateOfTransactionId() {
        return duplicateOfTransactionId;
    }

    public Instant getDuplicateOverriddenAt() {
        return duplicateOverriddenAt;
    }
}
