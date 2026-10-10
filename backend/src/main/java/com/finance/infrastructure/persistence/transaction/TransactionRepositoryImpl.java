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
        List<TransactionJpaEntity> found = externalReferences.isEmpty()
                ? jpaRepository.findDuplicateCandidatesByDate(userId, from, to)
                : jpaRepository.findDuplicateCandidates(userId, from, to, externalReferences);
        return found.stream().map(TransactionMapper::toDomain).toList();
    }

    @Override
    public List<Transaction> findConfirmedExpensesSince(UUID userId, java.time.LocalDate since) {
        return jpaRepository.findConfirmedExpensesSince(userId, since).stream().map(TransactionMapper::toDomain).toList();
    }

    @Override
    public List<Transaction> lookup(UUID userId, com.finance.domain.transaction.TransactionLookup lookup,
            com.finance.domain.transaction.TransactionLookup.Sort sort, int limit) {
        org.springframework.data.domain.Sort order = switch (sort) {
            case DATE_DESC -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("transactionDate"), org.springframework.data.domain.Sort.Order.desc("createdAt"));
            case DATE_ASC -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc("transactionDate"), org.springframework.data.domain.Sort.Order.asc("createdAt"));
            case AMOUNT_DESC -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("amount"), org.springframework.data.domain.Sort.Order.desc("transactionDate"));
            case AMOUNT_ASC -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc("amount"), org.springframework.data.domain.Sort.Order.desc("transactionDate"));
        };
        return jpaRepository.findAll(TransactionSpecifications.forLookup(userId, lookup), org.springframework.data.domain.PageRequest.of(0, limit, order))
                .stream().map(TransactionMapper::toDomain).toList();
    }

    @Override
    public long countLookup(UUID userId, com.finance.domain.transaction.TransactionLookup lookup) {
        return jpaRepository.count(TransactionSpecifications.forLookup(userId, lookup));
    }

    @Override
    public Page<Transaction> search(UUID userId, TransactionFilter filter, Pageable pageable) {
        return jpaRepository.findAll(TransactionSpecifications.forUserAndFilter(userId, filter), pageable).map(TransactionMapper::toDomain);
    }
}
