package com.finance.infrastructure.persistence.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, UUID>, JpaSpecificationExecutor<TransactionJpaEntity> {

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<TransactionJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    List<TransactionJpaEntity> findByTransferGroupIdAndUserId(UUID transferGroupId, UUID userId);

    @org.springframework.data.jpa.repository.Query("""
            select t from TransactionJpaEntity t
            where t.userId = :userId
              and t.status = com.finance.domain.transaction.TransactionStatus.CONFIRMED
              and ((t.transactionDate between :from and :to)
                   or (t.externalTransactionId is not null and t.externalTransactionId in :references))
            """)
    List<TransactionJpaEntity> findDuplicateCandidates(
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("from") java.time.LocalDate from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDate to,
            @org.springframework.data.repository.query.Param("references") java.util.Collection<String> references);
}
