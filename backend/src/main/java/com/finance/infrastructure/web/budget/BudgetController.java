package com.finance.infrastructure.web.budget;

import com.finance.application.budget.BudgetCategoryLimitCommand;
import com.finance.application.budget.BudgetService;
import com.finance.application.budget.SaveBudgetCommand;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
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
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final TenantContext tenantContext;

    public BudgetController(BudgetService budgetService, TenantContext tenantContext) {
        this.budgetService = budgetService;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ApiResponse<BudgetResponse> create(@Valid @RequestBody SaveBudgetRequest request) {
        return ApiResponse.of(BudgetResponse.from(budgetService.create(CurrentUserGuard.require(tenantContext), toCommand(request))));
    }

    @GetMapping
    public ApiResponse<List<BudgetResponse>> list() {
        return ApiResponse.of(budgetService.list(CurrentUserGuard.require(tenantContext)).stream().map(BudgetResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<BudgetResponse> get(@PathVariable UUID id) {
        return ApiResponse.of(BudgetResponse.from(budgetService.getOwned(CurrentUserGuard.require(tenantContext), id)));
    }

    @PutMapping("/{id}")
    public ApiResponse<BudgetResponse> update(@PathVariable UUID id, @Valid @RequestBody SaveBudgetRequest request) {
        return ApiResponse.of(BudgetResponse.from(budgetService.update(CurrentUserGuard.require(tenantContext), id, toCommand(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        budgetService.delete(CurrentUserGuard.require(tenantContext), id);
        return ResponseEntity.noContent().build();
    }

    private static SaveBudgetCommand toCommand(SaveBudgetRequest request) {
        List<BudgetCategoryLimitCommand> limits = request.categoryLimits() == null ? List.of()
                : request.categoryLimits().stream().map(l -> new BudgetCategoryLimitCommand(l.categoryId(), l.limitAmount())).toList();
        return new SaveBudgetCommand(
                request.name(), request.periodType(), request.startDate(), request.endDate(), request.totalLimit(), request.currency(), limits);
    }
}
