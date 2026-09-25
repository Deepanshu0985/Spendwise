package com.finance.infrastructure.tenancy;

import java.util.UUID;

/**
 * Write side of {@link TenantContext}, intended for exactly one caller: the
 * authentication filter that resolves the session and populates this per-request,
 * clearing it once the request completes. No other class should depend on this -
 * everything else depends on the read-only {@link TenantContext}.
 */
public interface MutableTenantContext extends TenantContext {

    void setCurrentUserId(UUID userId);

    void clear();
}
