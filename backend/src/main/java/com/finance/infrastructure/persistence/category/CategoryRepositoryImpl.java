package com.finance.infrastructure.persistence.category;

import com.finance.domain.category.Category;
import com.finance.domain.category.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpaRepository;

    public CategoryRepositoryImpl(CategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = jpaRepository.findById(category.getId())
                .map(existing -> CategoryMapper.applyChanges(existing, category))
                .orElseGet(() -> CategoryMapper.toNewEntity(category));
        return CategoryMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Category> findVisibleForUser(UUID userId) {
        return jpaRepository.findVisibleForUser(userId).stream().map(CategoryMapper::toDomain).toList();
    }

    @Override
    public Optional<Category> findByIdAndUserIdAndSystemFalse(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserIdAndSystemFalse(id, userId).map(CategoryMapper::toDomain);
    }

    @Override
    public Optional<Category> findVisibleByIdForUser(UUID id, UUID userId) {
        return jpaRepository.findVisibleByIdForUser(id, userId).map(CategoryMapper::toDomain);
    }
}
