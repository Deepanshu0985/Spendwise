package com.finance.infrastructure.web.category;

import com.finance.domain.category.Category;
import com.finance.domain.category.CategoryType;

import java.util.UUID;

public record CategoryResponse(
        UUID id, String name, CategoryType categoryType, UUID parentId, boolean system, boolean active) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCategoryType(),
                category.getParentId(),
                category.isSystem(),
                category.isActive());
    }
}
