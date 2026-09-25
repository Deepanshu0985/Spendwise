package com.finance.infrastructure.persistence.transaction;

import com.finance.domain.transaction.TransactionSplit;

/** Pure mapping logic, not swappable business behavior - no interface, same reasoning as CurrentUserGuard. */
final class TransactionSplitMapper {

    private TransactionSplitMapper() {
    }

    static TransactionSplit toDomain(TransactionSplitJpaEntity entity) {
        return new TransactionSplit(
                entity.getId(), entity.getUserId(), entity.getTransactionId(), entity.getCategoryId(), entity.getAmount(), entity.getCreatedAt());
    }

    static TransactionSplitJpaEntity toNewEntity(TransactionSplit split) {
        return new TransactionSplitJpaEntity(split.getId(), split.getUserId(), split.getTransactionId(), split.getCategoryId(), split.getAmount());
    }
}
