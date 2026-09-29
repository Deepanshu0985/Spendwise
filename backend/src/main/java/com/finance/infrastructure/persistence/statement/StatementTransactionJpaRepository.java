package com.finance.infrastructure.persistence.statement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatementTransactionJpaRepository extends JpaRepository<StatementTransactionJpaEntity, UUID> {

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<StatementTransactionJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    List<StatementTransactionJpaEntity> findByStatementIdAndUserId(UUID statementId, UUID userId);
}
