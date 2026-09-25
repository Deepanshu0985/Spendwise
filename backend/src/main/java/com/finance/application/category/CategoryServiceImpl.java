package com.finance.application.category;

import com.finance.application.exception.NotFoundException;
import com.finance.domain.category.Category;
import com.finance.domain.category.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Category create(UUID userId, CreateCategoryCommand command) {
        Category category = new Category(userId, command.name(), command.categoryType());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> listVisibleForUser(UUID userId) {
        return categoryRepository.findVisibleForUser(userId);
    }

    @Override
    @Transactional
    public Category rename(UUID userId, UUID categoryId, RenameCategoryCommand command) {
        Category category = findOwnedCustom(userId, categoryId);
        category.rename(command.name());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deactivate(UUID userId, UUID categoryId) {
        Category category = findOwnedCustom(userId, categoryId);
        category.deactivate();
        categoryRepository.save(category);
    }

    private Category findOwnedCustom(UUID userId, UUID categoryId) {
        // System categories are visible but never owned by a user - the same
        // lookup that would find "yours" correctly finds nothing for a system
        // row, so this doubles as the "not a system category" check.
        return categoryRepository.findByIdAndUserIdAndSystemFalse(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found."));
    }
}
