package com.finance.infrastructure.persistence.statement;

import com.finance.domain.statement.PayeeRule;
import com.finance.domain.statement.PayeeRuleRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PayeeRuleRepositoryImpl implements PayeeRuleRepository {

    private final PayeeRuleJpaRepository jpaRepository;

    public PayeeRuleRepositoryImpl(PayeeRuleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PayeeRule save(PayeeRule rule) {
        PayeeRuleJpaEntity entity = jpaRepository.findById(rule.getId())
                .map(existing -> {
                    existing.setMerchantId(rule.getMerchantId());
                    existing.setCategoryId(rule.getCategoryId());
                    return existing;
                })
                .orElseGet(() -> new PayeeRuleJpaEntity(
                        rule.getId(), rule.getUserId(), rule.getMatchKey(), rule.getMerchantId(), rule.getCategoryId()));
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<PayeeRule> findByUserIdAndMatchKey(UUID userId, String matchKey) {
        return jpaRepository.findByUserIdAndMatchKey(userId, matchKey).map(PayeeRuleRepositoryImpl::toDomain);
    }

    @Override
    public List<PayeeRule> findByUserIdAndMatchKeyIn(UUID userId, Collection<String> matchKeys) {
        return jpaRepository.findByUserIdAndMatchKeyIn(userId, matchKeys).stream().map(PayeeRuleRepositoryImpl::toDomain).toList();
    }

    private static PayeeRule toDomain(PayeeRuleJpaEntity entity) {
        return new PayeeRule(
                entity.getId(), entity.getUserId(), entity.getMatchKey(), entity.getMerchantId(), entity.getCategoryId(),
                entity.getCreatedAt());
    }
}
