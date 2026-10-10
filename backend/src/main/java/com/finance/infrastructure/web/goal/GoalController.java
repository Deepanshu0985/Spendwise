package com.finance.infrastructure.web.goal;

import com.finance.application.goal.GoalService;
import com.finance.application.goal.SaveGoalCommand;
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
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;
    private final TenantContext tenantContext;

    public GoalController(GoalService goalService, TenantContext tenantContext) {
        this.goalService = goalService;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ApiResponse<GoalResponse> create(@Valid @RequestBody SaveGoalRequest request) {
        return ApiResponse.of(GoalResponse.from(goalService.create(CurrentUserGuard.require(tenantContext), toCommand(request))));
    }

    @GetMapping
    public ApiResponse<List<GoalResponse>> list() {
        return ApiResponse.of(goalService.list(CurrentUserGuard.require(tenantContext)).stream().map(GoalResponse::from).toList());
    }

    @PutMapping("/{id}")
    public ApiResponse<GoalResponse> update(@PathVariable UUID id, @Valid @RequestBody SaveGoalRequest request) {
        return ApiResponse.of(GoalResponse.from(goalService.update(CurrentUserGuard.require(tenantContext), id, toCommand(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        goalService.delete(CurrentUserGuard.require(tenantContext), id);
        return ResponseEntity.noContent().build();
    }

    private static SaveGoalCommand toCommand(SaveGoalRequest request) {
        return new SaveGoalCommand(request.name(), request.targetAmount(), request.currentAmount(), request.targetDate(), request.currency());
    }
}
