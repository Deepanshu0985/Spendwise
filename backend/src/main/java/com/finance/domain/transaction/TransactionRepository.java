package com.finance.domain.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port - implemented by infrastructure.persistence.transaction.TransactionRepositoryImpl.
 * No JPA types here; Pageable/Page are Spring Data Commons pagination types (not
 * JPA-specific), kept as a pragmatic exception rather than inventing a parallel
 * domain pagination abstraction for no real benefit at this project's stage.
 */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    List<Transaction> findByTransferGroupIdAndUserId(UUID transferGroupId, UUID userId);

    /**
     * Confirmed transactions that could be the same real-world event as a staged row: dated inside the window
     * or sharing one of the given external references (checked across all accounts). Fetched once per
     * statement, not per row - each query is a network round trip.
     */
    List<Transaction> findDuplicateCandidates(
            UUID userId, java.time.LocalDate from, java.time.LocalDate to, java.util.Collection<String> externalReferences);

    /** Confirmed EXPENSE transactions dated on or after since - the history recurring-payment detection reads. */
    List<Transaction> findConfirmedExpensesSince(UUID userId, java.time.LocalDate since);

    /** Confirmed transactions matching the lookup, in the given order, at most limit of them. */
    List<Transaction> lookup(UUID userId, TransactionLookup lookup, TransactionLookup.Sort sort, int limit);

    /** How many confirmed transactions match the lookup. */
    long countLookup(UUID userId, TransactionLookup lookup);

    Page<Transaction> search(UUID userId, TransactionFilter filter, Pageable pageable);
}
