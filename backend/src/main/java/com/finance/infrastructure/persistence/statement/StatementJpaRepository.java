package com.finance.infrastructure.persistence.statement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StatementJpaRepository extends JpaRepository<StatementJpaEntity, UUID> {

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<StatementJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<StatementJpaEntity> findByUserIdAndFileHash(UUID userId, String fileHash);

    Page<StatementJpaEntity> findByUserId(UUID userId, Pageable pageable);
}
