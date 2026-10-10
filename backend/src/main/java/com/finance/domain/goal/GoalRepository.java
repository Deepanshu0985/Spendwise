package com.finance.domain.goal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.goal.GoalRepositoryImpl. */
public interface GoalRepository {

    Goal save(Goal goal);

    // Explicit user_id filtering here is intentional defense-in-depth alongside RLS (api-specification.md's Authorization section).
    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);

    List<Goal> findByUserId(UUID userId);

    void delete(Goal goal);
}
