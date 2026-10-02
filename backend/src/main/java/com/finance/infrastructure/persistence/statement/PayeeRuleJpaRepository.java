package com.finance.infrastructure.persistence.statement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayeeRuleJpaRepository extends JpaRepository<PayeeRuleJpaEntity, UUID> {

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<PayeeRuleJpaEntity> findByUserIdAndMatchKey(UUID userId, String matchKey);

    List<PayeeRuleJpaEntity> findByUserIdAndMatchKeyIn(UUID userId, Collection<String> matchKeys);
}
