package com.finance.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, UUID> {

    List<TransactionSplit> findByTransactionId(UUID transactionId);

    void deleteByTransactionId(UUID transactionId);
}
