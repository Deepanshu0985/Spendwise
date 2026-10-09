package com.finance.application.statement;

import com.finance.domain.statement.PayeeKey;
import com.finance.domain.statement.SourceAccountRule;
import com.finance.domain.statement.SourceAccountRuleRepository;
import com.finance.domain.statement.StatementTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SourceAccountRuleServiceImpl implements SourceAccountRuleService {

    private final SourceAccountRuleRepository ruleRepository;

    public SourceAccountRuleServiceImpl(SourceAccountRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional
    public void remember(UUID userId, String sourceAccountLabel, UUID accountId) {
        String key = PayeeKey.of(sourceAccountLabel);
        if (key.isEmpty()) {
            return;
        }
        SourceAccountRule rule = ruleRepository.findByUserIdAndLabelKey(userId, key)
                .orElseGet(() -> new SourceAccountRule(userId, key, accountId));
        rule.pointTo(accountId);
        ruleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementTransaction> applyTo(UUID userId, List<StatementTransaction> rows) {
        Set<String> keys = rows.stream()
                .filter(row -> row.getSourceAccountLabel() != null && row.getAccountId() == null)
                .map(row -> PayeeKey.of(row.getSourceAccountLabel()))
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toSet());
        if (keys.isEmpty()) {
            return List.of();
        }
        Map<String, SourceAccountRule> rules = ruleRepository.findByUserIdAndLabelKeyIn(userId, keys).stream()
                .collect(Collectors.toMap(SourceAccountRule::getLabelKey, Function.identity()));
        return rows.stream()
                .filter(row -> row.getSourceAccountLabel() != null && row.getAccountId() == null)
                .filter(row -> {
                    SourceAccountRule rule = rules.get(PayeeKey.of(row.getSourceAccountLabel()));
                    if (rule == null) {
                        return false;
                    }
                    row.assignAccount(rule.getAccountId());
                    return true;
                })
                .toList();
    }
}
