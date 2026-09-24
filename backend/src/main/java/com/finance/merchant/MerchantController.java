package com.finance.merchant;

import com.finance.common.ApiResponse;
import com.finance.common.CurrentUserGuard;
import com.finance.common.TenantContext;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;
    private final TenantContext tenantContext;

    public MerchantController(MerchantService merchantService, TenantContext tenantContext) {
        this.merchantService = merchantService;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ApiResponse<MerchantResponse> create(@Valid @RequestBody MerchantNameRequest request) {
        Merchant merchant = merchantService.create(CurrentUserGuard.require(tenantContext), request);
        return ApiResponse.of(MerchantResponse.from(merchant));
    }

    @GetMapping
    public ApiResponse<List<MerchantResponse>> list() {
        List<MerchantResponse> merchants = merchantService.listForUser(CurrentUserGuard.require(tenantContext)).stream()
                .map(MerchantResponse::from)
                .toList();
        return ApiResponse.of(merchants);
    }

    @PutMapping("/{id}")
    public ApiResponse<MerchantResponse> rename(@PathVariable UUID id, @Valid @RequestBody MerchantNameRequest request) {
        Merchant merchant = merchantService.rename(CurrentUserGuard.require(tenantContext), id, request);
        return ApiResponse.of(MerchantResponse.from(merchant));
    }
}
