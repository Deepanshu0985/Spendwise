package com.finance.infrastructure.tenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Issues SET LOCAL app.current_user_id at the start of every @Transactional
 * method, per lld.md's "Tenant Context Plumbing" and ADR-010. @Order(1), one
 * step inside the @Order(0) transactional advisor (TransactionManagementConfig),
 * so this runs after the transaction has begun and before the method body -
 * meaning the EntityManager here is bound to the same connection/transaction
 * the rest of the method's queries use.
 *
 * set_config(..., true) is the SQL-standard-compatible equivalent of SET LOCAL
 * and supports normal JDBC parameter binding, unlike SET LOCAL as raw SQL. When
 * there is no authenticated user, nothing is set - any SET LOCAL value from a
 * previous transaction on a reused pooled connection has already been reverted
 * by Postgres when that transaction ended, so unauthenticated requests correctly
 * see no value (see ADR-019's note on the pooled-connection leakage test).
 */
@Aspect
@Component
@Order(1)
public class TenantContextAspect {

    @PersistenceContext
    private EntityManager entityManager;

    private final TenantContext tenantContext;

    public TenantContextAspect(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Before("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
    public void setTenantContext() {
        UUID userId = tenantContext.currentUserId();
        if (userId != null) {
            entityManager.createNativeQuery("SELECT set_config('app.current_user_id', ?1, true)")
                    .setParameter(1, userId.toString())
                    .getSingleResult();
        }
    }
}
