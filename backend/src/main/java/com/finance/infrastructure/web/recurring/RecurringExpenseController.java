package com.finance.infrastructure.web.recurring;

import com.finance.application.recurring.RecurringExpenseService;
import com.finance.application.recurring.UpdateRecurringExpenseCommand;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recurring-expenses")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;
    private final TenantContext tenantContext;

    public RecurringExpenseController(RecurringExpenseService recurringExpenseService, TenantContext tenantContext) {
        this.recurringExpenseService = recurringExpenseService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public ApiResponse<List<RecurringExpenseResponse>> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.of(recurringExpenseService.list(CurrentUserGuard.require(tenantContext), activeOnly).stream()
                .map(RecurringExpenseResponse::from).toList());
    }

    /** Re-runs detection over the user's confirmed expenses and returns the refreshed list. */
    @PostMapping("/detect")
    public ApiResponse<List<RecurringExpenseResponse>> detect() {
        return ApiResponse.of(recurringExpenseService.detect(CurrentUserGuard.require(tenantContext)).stream()
                .map(RecurringExpenseResponse::from).toList());
    }

    @PutMapping("/{id}")
    public ApiResponse<RecurringExpenseResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateRecurringExpenseRequest request) {
        return ApiResponse.of(RecurringExpenseResponse.from(recurringExpenseService.update(
                CurrentUserGuard.require(tenantContext), id,
                new UpdateRecurringExpenseCommand(request.name(), request.categoryId(), request.confirmed()))));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable UUID id) {
        recurringExpenseService.dismiss(CurrentUserGuard.require(tenantContext), id);
        return ResponseEntity.noContent().build();
    }
}
