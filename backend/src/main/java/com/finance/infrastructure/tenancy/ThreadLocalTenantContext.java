package com.finance.infrastructure.tenancy;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Backed by a ThreadLocal, which is safe here because the authentication filter
 * sets it at the start of request processing on Tomcat's request-handling thread
 * and clears it in a finally block before that thread returns to the pool - it
 * never survives past the request it was set for.
 */
@Component
public class ThreadLocalTenantContext implements MutableTenantContext {

    private final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();

    @Override
    public UUID currentUserId() {
        return currentUserId.get();
    }

    @Override
    public void setCurrentUserId(UUID userId) {
        currentUserId.set(userId);
    }

    @Override
    public void clear() {
        currentUserId.remove();
    }
}
