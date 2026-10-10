package com.finance.infrastructure.web.insight;

import com.finance.application.insight.InsightService;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import com.finance.application.exception.DomainValidationException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/insights/monthly")
public class InsightController {

    private final InsightService insightService;
    private final TenantContext tenantContext;

    public InsightController(InsightService insightService, TenantContext tenantContext) {
        this.insightService = insightService;
        this.tenantContext = tenantContext;
    }

    /** The stored insight for a month, or null data if none has been written yet. Never calls a model. */
    @GetMapping
    public ApiResponse<InsightResponse> get(@RequestParam String month) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        return ApiResponse.of(insightService.find(userId, parse(month)).map(insight -> InsightResponse.from(insight, true, null)).orElse(null));
    }

    /** Writes (or reuses) the insight for a month; counts against the AI insight caps only when a model is actually called. */
    @PostMapping
    public ApiResponse<InsightResponse> generate(@Valid @RequestBody GenerateInsightRequest request) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        return ApiResponse.of(InsightResponse.from(insightService.generate(userId, parse(request.month()), Boolean.TRUE.equals(request.refresh()))));
    }

    private static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new DomainValidationException("The month must be written yyyy-mm.", List.of());
        }
    }
}
