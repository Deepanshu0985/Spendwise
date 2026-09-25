package com.finance.domain.transaction;

import java.util.List;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.transaction.TransactionSplitRepositoryImpl. No JPA types here. */
public interface TransactionSplitRepository {

    List<TransactionSplit> saveAll(List<TransactionSplit> splits);

    List<TransactionSplit> findByTransactionId(UUID transactionId);

    void deleteByTransactionId(UUID transactionId);
}
