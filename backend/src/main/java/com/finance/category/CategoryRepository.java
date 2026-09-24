package com.finance.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Mirrors the categories_select RLS policy in application-layer form
    // (defense-in-depth, not redundant with it - api-specification.md).
    @Query("SELECT c FROM Category c WHERE c.active = true AND (c.system = true OR c.userId = :userId) ORDER BY c.name")
    List<Category> findVisibleForUser(@Param("userId") UUID userId);

    // Excludes system rows explicitly: only a user's own custom categories are ever updatable/deletable.
    Optional<Category> findByIdAndUserIdAndSystemFalse(UUID id, UUID userId);
}
