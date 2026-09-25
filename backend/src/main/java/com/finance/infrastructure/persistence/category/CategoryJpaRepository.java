package com.finance.infrastructure.persistence.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    // Mirrors the categories_select RLS policy in application-layer form
    // (defense-in-depth, not redundant with it - api-specification.md).
    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.active = true AND (c.system = true OR c.userId = :userId) ORDER BY c.name")
    List<CategoryJpaEntity> findVisibleForUser(@Param("userId") UUID userId);

    // Excludes system rows explicitly: only a user's own custom categories are ever updatable/deletable.
    Optional<CategoryJpaEntity> findByIdAndUserIdAndSystemFalse(UUID id, UUID userId);

    // For validating a categoryId referenced on a transaction/split: usable if
    // it's a shared system category or the user's own, active either way.
    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.id = :id AND c.active = true AND (c.system = true OR c.userId = :userId)")
    Optional<CategoryJpaEntity> findVisibleByIdForUser(@Param("id") UUID id, @Param("userId") UUID userId);
}
