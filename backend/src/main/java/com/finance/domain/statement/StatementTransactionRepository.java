package com.finance.domain.statement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.statement.StatementTransactionRepositoryImpl. */
public interface StatementTransactionRepository {

    StatementTransaction save(StatementTransaction row);

    List<StatementTransaction> saveAll(List<StatementTransaction> rows);

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<StatementTransaction> findByIdAndUserId(UUID id, UUID userId);

    List<StatementTransaction> findByStatementIdAndUserId(UUID statementId, UUID userId);
}
