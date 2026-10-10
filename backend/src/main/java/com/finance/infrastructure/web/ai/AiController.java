package com.finance.infrastructure.web.ai;

import com.finance.application.ai.CategorySuggestionService;
import com.finance.application.assistant.AssistantService;
import com.finance.application.assistant.AssistantTurn;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final CategorySuggestionService suggestionService;
    private final AssistantService assistantService;
    private final TenantContext tenantContext;

    public AiController(CategorySuggestionService suggestionService, AssistantService assistantService, TenantContext tenantContext) {
        this.suggestionService = suggestionService;
        this.assistantService = assistantService;
        this.tenantContext = tenantContext;
    }

    /** Lets the screen decide whether to offer AI suggestions, and say how many rows are left today. */
    @GetMapping("/status")
    public ApiResponse<CategorySuggestionService.AiStatus> status() {
        return ApiResponse.of(suggestionService.status(CurrentUserGuard.require(tenantContext)));
    }

    /** Asks the assistant a question. The caller sends the conversation so far; nothing is stored on the server. */
    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.of(ChatResponse.from(assistantService.chat(
                CurrentUserGuard.require(tenantContext),
                request.messages().stream().map(turn -> new AssistantTurn(turn.role(), turn.content())).toList())));
    }
}
