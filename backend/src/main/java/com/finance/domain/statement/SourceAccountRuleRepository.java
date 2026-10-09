package com.finance.domain.statement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.statement.SourceAccountRuleRepositoryImpl. */
public interface SourceAccountRuleRepository {

    SourceAccountRule save(SourceAccountRule rule);

    Optional<SourceAccountRule> findByUserIdAndLabelKey(UUID userId, String labelKey);

    List<SourceAccountRule> findByUserIdAndLabelKeyIn(UUID userId, Collection<String> labelKeys);
}
