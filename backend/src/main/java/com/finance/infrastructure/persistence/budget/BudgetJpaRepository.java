package com.finance.infrastructure.persistence.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, UUID> {

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<BudgetJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    List<BudgetJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
