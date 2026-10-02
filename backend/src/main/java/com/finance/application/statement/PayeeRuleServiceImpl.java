package com.finance.application.statement;

import com.finance.domain.statement.PayeeKey;
import com.finance.domain.statement.PayeeRule;
import com.finance.domain.statement.PayeeRuleRepository;
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
public class PayeeRuleServiceImpl implements PayeeRuleService {

    private final PayeeRuleRepository payeeRuleRepository;

    public PayeeRuleServiceImpl(PayeeRuleRepository payeeRuleRepository) {
        this.payeeRuleRepository = payeeRuleRepository;
    }

    @Override
    @Transactional
    public void remember(UUID userId, StatementTransaction reviewedRow) {
        String key = PayeeKey.of(displayText(reviewedRow));
        UUID merchantId = reviewedRow.getSuggestedMerchantId();
        UUID categoryId = reviewedRow.getSuggestedCategoryId();
        if (key.isEmpty() || (merchantId == null && categoryId == null)) {
            return;
        }
        PayeeRule rule = payeeRuleRepository.findByUserIdAndMatchKey(userId, key)
                .orElseGet(() -> new PayeeRule(userId, key, merchantId, categoryId));
        rule.update(merchantId, categoryId);
        payeeRuleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementTransaction> applyTo(UUID userId, List<StatementTransaction> rows) {
        Set<String> keys = rows.stream().map(row -> PayeeKey.of(displayText(row))).filter(key -> !key.isEmpty()).collect(Collectors.toSet());
        if (keys.isEmpty()) {
            return List.of();
        }
        Map<String, PayeeRule> rules = payeeRuleRepository.findByUserIdAndMatchKeyIn(userId, keys).stream()
                .collect(Collectors.toMap(PayeeRule::getMatchKey, Function.identity()));
        return rows.stream()
                .filter(row -> {
                    PayeeRule rule = rules.get(PayeeKey.of(displayText(row)));
                    return rule != null && row.applySuggestion(rule.getMerchantId(), rule.getCategoryId());
                })
                .toList();
    }

    private static String displayText(StatementTransaction row) {
        return row.getNormalizedDescription() != null ? row.getNormalizedDescription() : row.getRawDescription();
    }
}
