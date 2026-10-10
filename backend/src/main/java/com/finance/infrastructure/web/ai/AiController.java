package com.finance.infrastructure.web.ai;

import com.finance.application.ai.CategorySuggestionService;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final CategorySuggestionService suggestionService;
    private final TenantContext tenantContext;

    public AiController(CategorySuggestionService suggestionService, TenantContext tenantContext) {
        this.suggestionService = suggestionService;
        this.tenantContext = tenantContext;
    }

    /** Lets the screen decide whether to offer AI suggestions, and say how many rows are left today. */
    @GetMapping("/status")
    public ApiResponse<CategorySuggestionService.AiStatus> status() {
        return ApiResponse.of(suggestionService.status(CurrentUserGuard.require(tenantContext)));
    }
}
