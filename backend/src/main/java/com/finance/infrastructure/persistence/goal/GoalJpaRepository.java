package com.finance.infrastructure.persistence.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalJpaRepository extends JpaRepository<GoalJpaEntity, UUID> {

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<GoalJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    List<GoalJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
