package com.finance.common;

import com.finance.common.exception.UnauthorizedException;

import java.util.UUID;

/**
 * A pure guard clause, not swappable business behavior - no interface, same
 * reasoning as SessionCookieFactory. Extracted once the "throw if
 * unauthenticated" pattern started repeating across controllers (auth, user,
 * account, category, merchant).
 */
public final class CurrentUserGuard {

    private CurrentUserGuard() {
    }

    public static UUID require(TenantContext tenantContext) {
        UUID userId = tenantContext.currentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Not authenticated.");
        }
        return userId;
    }
}
