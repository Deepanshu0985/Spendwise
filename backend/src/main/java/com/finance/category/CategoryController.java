package com.finance.category;

import com.finance.common.ApiResponse;
import com.finance.common.CurrentUserGuard;
import com.finance.common.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final TenantContext tenantContext;

    public CategoryController(CategoryService categoryService, TenantContext tenantContext) {
        this.categoryService = categoryService;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.create(CurrentUserGuard.require(tenantContext), request);
        return ApiResponse.of(CategoryResponse.from(category));
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        List<CategoryResponse> categories = categoryService.listVisibleForUser(CurrentUserGuard.require(tenantContext)).stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.of(categories);
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> rename(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        Category category = categoryService.rename(CurrentUserGuard.require(tenantContext), id, request);
        return ApiResponse.of(CategoryResponse.from(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        categoryService.deactivate(CurrentUserGuard.require(tenantContext), id);
        return ResponseEntity.noContent().build();
    }
}
