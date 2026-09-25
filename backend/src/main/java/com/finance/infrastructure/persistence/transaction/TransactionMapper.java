package com.finance.infrastructure.persistence.transaction;

import com.finance.domain.transaction.Transaction;

/** Pure mapping logic, not swappable business behavior - no interface, same reasoning as CurrentUserGuard. */
final class TransactionMapper {

    private TransactionMapper() {
    }

    static Transaction toDomain(TransactionJpaEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getUserId(),
                entity.getAccountId(),
                entity.getMerchantId(),
                entity.getCategoryId(),
                entity.getTransactionDate(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getDescription(),
                entity.getRawDescription(),
                entity.getTransactionType(),
                entity.getPaymentMethod(),
                entity.getSource(),
                entity.getSourceReference(),
                entity.getExternalTransactionId(),
                entity.getTransferGroupId(),
                entity.getConfidenceScore(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static TransactionJpaEntity toNewEntity(Transaction transaction) {
        return new TransactionJpaEntity(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getAccountId(),
                transaction.getMerchantId(),
                transaction.getCategoryId(),
                transaction.getTransactionDate(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getRawDescription(),
                transaction.getTransactionType(),
                transaction.getPaymentMethod(),
                transaction.getSource(),
                transaction.getSourceReference(),
                transaction.getExternalTransactionId(),
                transaction.getTransferGroupId(),
                transaction.getConfidenceScore(),
                transaction.getStatus());
    }

    /** Mutates an existing managed entity in place so Hibernate's dirty checking (and @UpdateTimestamp) fires correctly. */
    static TransactionJpaEntity applyChanges(TransactionJpaEntity entity, Transaction transaction) {
        entity.setAccountId(transaction.getAccountId());
        entity.setMerchantId(transaction.getMerchantId());
        entity.setCategoryId(transaction.getCategoryId());
        entity.setTransactionDate(transaction.getTransactionDate());
        entity.setAmount(transaction.getAmount());
        entity.setDescription(transaction.getDescription());
        entity.setStatus(transaction.getStatus());
        entity.setTransferGroupId(transaction.getTransferGroupId());
        return entity;
    }
}
