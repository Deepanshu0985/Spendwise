package com.finance.application.transaction;

import com.finance.domain.transaction.TransactionFilter;
import com.finance.domain.transaction.TransactionWithSplits;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionService {

    TransactionWithSplits create(UUID userId, CreateTransactionCommand command);

    Page<TransactionWithSplits> list(UUID userId, TransactionFilter filter, Pageable pageable);

    TransactionWithSplits getOwned(UUID userId, UUID transactionId);

    TransactionWithSplits update(UUID userId, UUID transactionId, UpdateTransactionCommand command);

    void softDelete(UUID userId, UUID transactionId);
}
