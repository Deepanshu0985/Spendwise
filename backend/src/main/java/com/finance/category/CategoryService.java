package com.finance.category;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    Category create(UUID userId, CreateCategoryRequest request);

    List<Category> listVisibleForUser(UUID userId);

    Category rename(UUID userId, UUID categoryId, UpdateCategoryRequest request);

    void deactivate(UUID userId, UUID categoryId);
}
