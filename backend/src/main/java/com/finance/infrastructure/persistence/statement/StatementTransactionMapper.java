package com.finance.infrastructure.persistence.statement;

import com.finance.domain.statement.StatementTransaction;

/** Pure mapping logic, not swappable business behavior - no interface, same reasoning as CurrentUserGuard. */
final class StatementTransactionMapper {

    private StatementTransactionMapper() {
    }

    static StatementTransaction toDomain(StatementTransactionJpaEntity entity) {
        return new StatementTransaction(
                entity.getId(),
                entity.getUserId(),
                entity.getStatementId(),
                entity.getTransactionDate(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getRawDescription(),
                entity.getNormalizedDescription(),
                entity.getSuggestedMerchantId(),
                entity.getSuggestedCategoryId(),
                entity.getSuggestedTransactionType(),
                entity.getConfidenceScore(),
                entity.getDuplicateStatus(),
                entity.getReviewStatus(),
                entity.getCanonicalTransactionId(),
                entity.getSourceRowReference(),
                entity.getCreatedAt(),
                entity.getExternalReference(),
                entity.getDuplicateReason(),
                entity.getDuplicateOfTransactionId(),
                entity.getDuplicateOverriddenAt());
    }

    static StatementTransactionJpaEntity toNewEntity(StatementTransaction row) {
        return new StatementTransactionJpaEntity(
                row.getId(),
                row.getUserId(),
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
                row.getSourceRowReference(),
                row.getExternalReference(),
                row.getDuplicateReason(),
                row.getDuplicateOfTransactionId(),
                row.getDuplicateOverriddenAt());
    }

    /** Mutates an existing managed entity in place so Hibernate's dirty checking fires correctly. */
    static StatementTransactionJpaEntity applyChanges(StatementTransactionJpaEntity entity, StatementTransaction row) {
        entity.setTransactionDate(row.getTransactionDate());
        entity.setAmount(row.getAmount());
        entity.setSuggestedMerchantId(row.getSuggestedMerchantId());
        entity.setSuggestedCategoryId(row.getSuggestedCategoryId());
        entity.setSuggestedTransactionType(row.getSuggestedTransactionType());
        entity.setReviewStatus(row.getReviewStatus());
        entity.setCanonicalTransactionId(row.getCanonicalTransactionId());
        entity.setDuplicateStatus(row.getDuplicateStatus());
        entity.setDuplicateReason(row.getDuplicateReason());
        entity.setDuplicateOfTransactionId(row.getDuplicateOfTransactionId());
        entity.setDuplicateOverriddenAt(row.getDuplicateOverriddenAt());
        return entity;
    }
}
