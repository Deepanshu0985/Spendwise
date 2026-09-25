package com.finance.application.category;

import com.finance.domain.category.CategoryType;

/** Application-layer input, distinct from the HTTP request DTO - mapped by infrastructure.web.category.CategoryController. */
public record CreateCategoryCommand(String name, CategoryType categoryType) {
}
