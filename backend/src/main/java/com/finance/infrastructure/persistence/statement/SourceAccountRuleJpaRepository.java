package com.finance.infrastructure.persistence.statement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceAccountRuleJpaRepository extends JpaRepository<SourceAccountRuleJpaEntity, UUID> {

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<SourceAccountRuleJpaEntity> findByUserIdAndLabelKey(UUID userId, String labelKey);

    List<SourceAccountRuleJpaEntity> findByUserIdAndLabelKeyIn(UUID userId, Collection<String> labelKeys);
}
