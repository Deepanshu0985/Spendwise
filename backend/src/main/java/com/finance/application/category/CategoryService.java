package com.finance.application.category;

import com.finance.domain.category.Category;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    Category create(UUID userId, CreateCategoryCommand command);

    List<Category> listVisibleForUser(UUID userId);

    Category rename(UUID userId, UUID categoryId, RenameCategoryCommand command);

    void deactivate(UUID userId, UUID categoryId);
}
