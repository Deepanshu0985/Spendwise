package com.finance.infrastructure.tenancy;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * order = 0 makes the transactional advisor the outermost aspect around any
 * @Transactional method, so it starts the database transaction before
 * {@link TenantContextAspect} (a plain @Order(1) bean, therefore innermost) runs.
 * That ordering is what lets the aspect's SET LOCAL land inside the same
 * transaction/connection the rest of the method's queries will use.
 */
@Configuration
@EnableTransactionManagement(order = 0)
public class TransactionManagementConfig {
}
