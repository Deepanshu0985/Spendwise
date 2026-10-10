package com.finance.infrastructure.web.statement;

import com.finance.domain.statement.DuplicateReason;
import com.finance.domain.statement.DuplicateStatus;
import com.finance.domain.statement.ReviewStatus;
import com.finance.domain.statement.StatementTransaction;
import com.finance.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StatementTransactionResponse(
        UUID id,
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
        DuplicateReason duplicateReason,
        UUID duplicateOfTransactionId,
        boolean duplicateOverridden,
        String sourceAccountLabel,
        UUID accountId,
        boolean aiSuggested,
        String aiReason) {

    public static StatementTransactionResponse from(StatementTransaction row) {
        return new StatementTransactionResponse(
                row.getId(),
                row.getStatementId(),
                row.getTransactionDate(),
                row.getAmount(),
                row.getCurrency(),
                row.getRawDescription(),
                row.getNormalizedDescription(),
                row.getSuggestedMerchantId(),
                row.getSuggestedCategoryId(),
                row.getSuggestedTransactionType(),
                row.getConfidenceScore(),
                row.getDuplicateStatus(),
                row.getReviewStatus(),
                row.getCanonicalTransactionId(),
                row.getDuplicateReason(),
                row.getDuplicateOfTransactionId(),
                row.getDuplicateOverriddenAt() != null,
                row.getSourceAccountLabel(),
                row.getAccountId(),
                row.isAiSuggested(),
                row.getAiReason());
    }
}
