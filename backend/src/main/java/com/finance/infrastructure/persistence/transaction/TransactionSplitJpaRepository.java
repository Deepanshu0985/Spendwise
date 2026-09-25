package com.finance.infrastructure.persistence.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionSplitJpaRepository extends JpaRepository<TransactionSplitJpaEntity, UUID> {

    List<TransactionSplitJpaEntity> findByTransactionId(UUID transactionId);

    void deleteByTransactionId(UUID transactionId);
}
