package com.finance.infrastructure.persistence.category;

import com.finance.domain.category.Category;

/** Pure mapping logic, not swappable business behavior - no interface, same reasoning as CurrentUserGuard. */
final class CategoryMapper {

    private CategoryMapper() {
    }

    static Category toDomain(CategoryJpaEntity entity) {
        return new Category(
                entity.getId(),
                entity.getUserId(),
                entity.getParentId(),
                entity.getName(),
                entity.getCategoryType(),
                entity.isSystem(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static CategoryJpaEntity toNewEntity(Category category) {
        return new CategoryJpaEntity(
                category.getId(),
                category.getUserId(),
                category.getParentId(),
                category.getName(),
                category.getCategoryType(),
                category.isSystem(),
                category.isActive());
    }

    /** Mutates an existing managed entity in place so Hibernate's dirty checking (and @UpdateTimestamp) fires correctly. */
    static CategoryJpaEntity applyChanges(CategoryJpaEntity entity, Category category) {
        entity.setName(category.getName());
        entity.setActive(category.isActive());
        return entity;
    }
}
