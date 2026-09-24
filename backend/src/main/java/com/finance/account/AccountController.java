package com.finance.account;

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
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TenantContext tenantContext;

    public AccountController(AccountService accountService, TenantContext tenantContext) {
        this.accountService = accountService;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ApiResponse<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(CurrentUserGuard.require(tenantContext), request);
        return ApiResponse.of(AccountResponse.from(account));
    }

    @GetMapping
    public ApiResponse<List<AccountResponse>> list() {
        List<AccountResponse> accounts = accountService.listForUser(CurrentUserGuard.require(tenantContext)).stream()
                .map(AccountResponse::from)
                .toList();
        return ApiResponse.of(accounts);
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> get(@PathVariable UUID id) {
        Account account = accountService.getOwned(CurrentUserGuard.require(tenantContext), id);
        return ApiResponse.of(AccountResponse.from(account));
    }

    @PutMapping("/{id}")
    public ApiResponse<AccountResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest request) {
        Account account = accountService.update(CurrentUserGuard.require(tenantContext), id, request);
        return ApiResponse.of(AccountResponse.from(account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        accountService.deactivate(CurrentUserGuard.require(tenantContext), id);
        return ResponseEntity.noContent().build();
    }
}
