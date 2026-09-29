package com.finance.domain.statement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.statement.StatementRepositoryImpl. */
public interface StatementRepository {

    Statement save(Statement statement);

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<Statement> findByIdAndUserId(UUID id, UUID userId);

    Optional<Statement> findByUserIdAndFileHash(UUID userId, String fileHash);

    Page<Statement> findByUserId(UUID userId, Pageable pageable);
}
