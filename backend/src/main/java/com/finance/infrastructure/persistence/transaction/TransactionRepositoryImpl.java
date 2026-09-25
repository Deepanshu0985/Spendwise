package com.finance.infrastructure.persistence.transaction;

import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionFilter;
import com.finance.domain.transaction.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;

    public TransactionRepositoryImpl(TransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = jpaRepository.findById(transaction.getId())
                .map(existing -> TransactionMapper.applyChanges(existing, transaction))
                .orElseGet(() -> TransactionMapper.toNewEntity(transaction));
        return TransactionMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Transaction> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(TransactionMapper::toDomain);
    }

    @Override
    public List<Transaction> findByTransferGroupIdAndUserId(UUID transferGroupId, UUID userId) {
        return jpaRepository.findByTransferGroupIdAndUserId(transferGroupId, userId).stream().map(TransactionMapper::toDomain).toList();
    }

    @Override
    public Page<Transaction> search(UUID userId, TransactionFilter filter, Pageable pageable) {
        return jpaRepository.findAll(TransactionSpecifications.forUserAndFilter(userId, filter), pageable).map(TransactionMapper::toDomain);
    }
}
