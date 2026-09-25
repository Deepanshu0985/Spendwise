package com.finance.domain.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.category.CategoryRepositoryImpl. No Spring Data / JPA types here. */
public interface CategoryRepository {

    Category save(Category category);

    List<Category> findVisibleForUser(UUID userId);

    Optional<Category> findByIdAndUserIdAndSystemFalse(UUID id, UUID userId);

    Optional<Category> findVisibleByIdForUser(UUID id, UUID userId);
}
