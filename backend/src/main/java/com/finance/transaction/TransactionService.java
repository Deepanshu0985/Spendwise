package com.finance.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionService {

    TransactionWithSplits create(UUID userId, CreateTransactionRequest request);

    Page<TransactionWithSplits> list(UUID userId, TransactionFilter filter, Pageable pageable);

    TransactionWithSplits getOwned(UUID userId, UUID transactionId);

    TransactionWithSplits update(UUID userId, UUID transactionId, UpdateTransactionRequest request);

    void softDelete(UUID userId, UUID transactionId);
}
