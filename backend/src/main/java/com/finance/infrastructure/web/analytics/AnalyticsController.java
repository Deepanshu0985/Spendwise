package com.finance.infrastructure.web.analytics;

import com.finance.application.analytics.AnalyticsService;
import com.finance.application.analytics.CategoryBreakdownView;
import com.finance.application.analytics.MerchantBreakdownView;
import com.finance.application.analytics.MonthlySummaryView;
import com.finance.application.analytics.TrendView;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * `from`/`to` are required, not defaulted to "this month" - determining "now"
 * needs a timezone reference (users.timezone governs display/scheduling only,
 * never transaction bucketing - analytics-specification.md's Period Semantics),
 * and the frontend is better placed to know its own local "today" than the
 * backend is. Month-over-month comparison is not a separate concern here: two
 * calls to /monthly, or reading adjacent points from /trends, both give it.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final TenantContext tenantContext;

    public AnalyticsController(AnalyticsService analyticsService, TenantContext tenantContext) {
        this.analyticsService = analyticsService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/monthly")
    public ApiResponse<MonthlySummaryResponse> monthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String currency) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        MonthlySummaryView view = analyticsService.monthlySummary(userId, from, to, currency);
        return ApiResponse.of(MonthlySummaryResponse.of(view.currency(), view.figures()), AnalyticsMeta.of(view.currency(), view.exclusion()));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryEntryResponse>> categories(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String currency) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        CategoryBreakdownView view = analyticsService.categoryBreakdown(userId, from, to, currency);
        List<CategoryEntryResponse> data = view.entries().stream().map(CategoryEntryResponse::from).toList();
        return ApiResponse.of(data, AnalyticsMeta.of(view.currency(), view.exclusion()));
    }

    @GetMapping("/merchants")
    public ApiResponse<List<MerchantEntryResponse>> merchants(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String currency) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        MerchantBreakdownView view = analyticsService.merchantBreakdown(userId, from, to, currency);
        List<MerchantEntryResponse> data = view.entries().stream().map(MerchantEntryResponse::from).toList();
        return ApiResponse.of(data, AnalyticsMeta.of(view.currency(), view.exclusion()));
    }

    @GetMapping("/trends")
    public ApiResponse<List<TrendPointResponse>> trends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String currency) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        TrendView view = analyticsService.trend(userId, from, to, currency);
        List<TrendPointResponse> data = view.points().stream().map(TrendPointResponse::from).toList();
        return ApiResponse.of(data, AnalyticsMeta.of(view.currency(), view.exclusion()));
    }
}
