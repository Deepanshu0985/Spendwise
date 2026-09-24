package com.finance.category;

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
