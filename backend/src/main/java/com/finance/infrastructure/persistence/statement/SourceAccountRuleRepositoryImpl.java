package com.finance.infrastructure.persistence.statement;

import com.finance.domain.statement.SourceAccountRule;
import com.finance.domain.statement.SourceAccountRuleRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SourceAccountRuleRepositoryImpl implements SourceAccountRuleRepository {

    private final SourceAccountRuleJpaRepository jpaRepository;

    public SourceAccountRuleRepositoryImpl(SourceAccountRuleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SourceAccountRule save(SourceAccountRule rule) {
        SourceAccountRuleJpaEntity entity = jpaRepository.findById(rule.getId())
                .map(existing -> {
                    existing.setAccountId(rule.getAccountId());
                    return existing;
                })
                .orElseGet(() -> new SourceAccountRuleJpaEntity(rule.getId(), rule.getUserId(), rule.getLabelKey(), rule.getAccountId()));
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<SourceAccountRule> findByUserIdAndLabelKey(UUID userId, String labelKey) {
        return jpaRepository.findByUserIdAndLabelKey(userId, labelKey).map(SourceAccountRuleRepositoryImpl::toDomain);
    }

    @Override
    public List<SourceAccountRule> findByUserIdAndLabelKeyIn(UUID userId, Collection<String> labelKeys) {
        return jpaRepository.findByUserIdAndLabelKeyIn(userId, labelKeys).stream().map(SourceAccountRuleRepositoryImpl::toDomain).toList();
    }

    private static SourceAccountRule toDomain(SourceAccountRuleJpaEntity entity) {
        return new SourceAccountRule(entity.getId(), entity.getUserId(), entity.getLabelKey(), entity.getAccountId());
    }
}
