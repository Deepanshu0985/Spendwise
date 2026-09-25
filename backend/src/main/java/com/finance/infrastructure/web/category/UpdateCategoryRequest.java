package com.finance.infrastructure.web.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Only the name is editable; categoryType stays fixed once set, same reasoning as accountType/currency on accounts. */
public record UpdateCategoryRequest(@NotBlank @Size(max = 255) String name) {
}
