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
    public List<Transaction> findDuplicateCandidates(
            UUID userId, java.time.LocalDate from, java.time.LocalDate to, java.util.Collection<String> externalReferences) {
        // An empty IN () list is rejected by some providers; a sentinel that never matches keeps the query valid.
        java.util.Collection<String> references = externalReferences.isEmpty() ? List.of("\u0000") : externalReferences;
        return jpaRepository.findDuplicateCandidates(userId, from, to, references).stream().map(TransactionMapper::toDomain).toList();
    }

    @Override
    public Page<Transaction> search(UUID userId, TransactionFilter filter, Pageable pageable) {
        return jpaRepository.findAll(TransactionSpecifications.forUserAndFilter(userId, filter), pageable).map(TransactionMapper::toDomain);
    }
}
