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

    Page<Transaction> search(UUID userId, TransactionFilter filter, Pageable pageable);
}
