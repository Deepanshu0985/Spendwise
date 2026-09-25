package com.finance.infrastructure.web.category;

import com.finance.domain.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(@NotBlank @Size(max = 255) String name, @NotNull CategoryType categoryType) {
}
