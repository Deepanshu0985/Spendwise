package com.finance.domain.statement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.statement.PayeeRuleRepositoryImpl. */
public interface PayeeRuleRepository {

    PayeeRule save(PayeeRule rule);

    Optional<PayeeRule> findByUserIdAndMatchKey(UUID userId, String matchKey);

    List<PayeeRule> findByUserIdAndMatchKeyIn(UUID userId, Collection<String> matchKeys);
}
