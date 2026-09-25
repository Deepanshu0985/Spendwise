package com.finance.infrastructure.persistence.transaction;

import com.finance.domain.transaction.TransactionSplit;
import com.finance.domain.transaction.TransactionSplitRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class TransactionSplitRepositoryImpl implements TransactionSplitRepository {

    private final TransactionSplitJpaRepository jpaRepository;

    public TransactionSplitRepositoryImpl(TransactionSplitJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<TransactionSplit> saveAll(List<TransactionSplit> splits) {
        // Splits are immutable once created (ADR-013) - callers always delete-and-recreate rather than edit, so every save is a fresh insert.
        List<TransactionSplitJpaEntity> entities = splits.stream().map(TransactionSplitMapper::toNewEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(TransactionSplitMapper::toDomain).toList();
    }

    @Override
    public List<TransactionSplit> findByTransactionId(UUID transactionId) {
        return jpaRepository.findByTransactionId(transactionId).stream().map(TransactionSplitMapper::toDomain).toList();
    }

    @Override
    public void deleteByTransactionId(UUID transactionId) {
        jpaRepository.deleteByTransactionId(transactionId);
    }
}
