package com.finance.common;

import java.util.UUID;

/**
 * Read-only view of the authenticated user for the current request, per lld.md.
 * Returns null when the request is not authenticated - Row-Level Security then
 * correctly returns nothing for any RLS-protected table (ADR-010).
 */
public interface TenantContext {

    UUID currentUserId();
}
