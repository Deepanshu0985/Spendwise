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
}
