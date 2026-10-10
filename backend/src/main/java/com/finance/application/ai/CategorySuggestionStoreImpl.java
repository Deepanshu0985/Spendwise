package com.finance.application.ai;

import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.NotFoundException;
import com.finance.domain.ai.AiRowInput;
import com.finance.domain.ai.AiSuggestion;
import com.finance.domain.ai.KeywordCategoryRules;
import com.finance.domain.ai.PiiRedactor;
import com.finance.domain.category.Category;
import com.finance.domain.category.CategoryRepository;
import com.finance.domain.category.CategoryType;
import com.finance.domain.statement.ReviewStatus;
import com.finance.domain.statement.Statement;
import com.finance.domain.statement.StatementRepository;
import com.finance.domain.statement.StatementStatus;
import com.finance.domain.statement.StatementTransaction;
import com.finance.domain.statement.StatementTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategorySuggestionStoreImpl implements CategorySuggestionStore {

    private final StatementRepository statementRepository;
    private final StatementTransactionRepository rowRepository;
    private final CategoryRepository categoryRepository;

    public CategorySuggestionStoreImpl(
            StatementRepository statementRepository, StatementTransactionRepository rowRepository, CategoryRepository categoryRepository) {
        this.statementRepository = statementRepository;
        this.rowRepository = rowRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public CategorySuggestionPlan prepare(UUID userId, UUID statementId, boolean forModel, int maxRows) {
        requireAwaitingReview(userId, statementId);
        List<StatementTransaction> rows = rowRepository.findByStatementIdAndUserId(statementId, userId);
        List<Category> categories = categoryRepository.findVisibleForUser(userId).stream().filter(Category::isActive).toList();
        Map<String, Category> byName = categories.stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(Locale.ROOT), c -> c, (first, second) -> first));

        int ruleApplied = 0;
        for (StatementTransaction row : eligible(rows)) {
            Category match = KeywordCategoryRules.categoryNameFor(row.getNormalizedDescription(), row.getRawDescription())
                    .map(name -> byName.get(name.toLowerCase(Locale.ROOT)))
                    .filter(category -> category.getCategoryType() == expectedKind(row))
                    .orElse(null);
            if (match != null && row.applySuggestion(null, match.getId())) {
                ruleApplied++;
            }
        }
        rowRepository.saveAll(rows);

        List<StatementTransaction> stillOpen = eligible(rows);
        List<AiRowInput> toAsk = forModel
                ? stillOpen.stream().limit(maxRows).map(row -> new AiRowInput(row.getId(), textFor(row), isDebit(row))).toList()
                : List.of();
        int beyond = forModel ? Math.max(0, stillOpen.size() - toAsk.size()) : 0;
        return new CategorySuggestionPlan(ruleApplied, toAsk, beyond, categories);
    }

    @Override
    @Transactional
    public int apply(UUID userId, UUID statementId, List<AiSuggestion> suggestions, double threshold) {
        requireAwaitingReview(userId, statementId);
        Map<UUID, StatementTransaction> byId = rowRepository.findByStatementIdAndUserId(statementId, userId).stream()
                .collect(Collectors.toMap(StatementTransaction::getId, row -> row));
        List<StatementTransaction> changed = new java.util.ArrayList<>();
        for (AiSuggestion suggestion : suggestions) {
            StatementTransaction row = byId.get(suggestion.rowId());
            if (row != null && suggestion.confidence() >= threshold
                    && row.applyAiSuggestion(suggestion.categoryId(), suggestion.type(), suggestion.reason())) {
                changed.add(row);
            }
        }
        rowRepository.saveAll(changed);
        return changed.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementTransaction> rows(UUID userId, UUID statementId) {
        return rowRepository.findByStatementIdAndUserId(statementId, userId);
    }

    private Statement requireAwaitingReview(UUID userId, UUID statementId) {
        Statement statement = statementRepository.findByIdAndUserId(statementId, userId)
                .orElseThrow(() -> new NotFoundException("Statement not found."));
        if (statement.getStatus() != StatementStatus.READY_FOR_REVIEW) {
            throw new DomainValidationException(
                    "Categories can only be suggested while the statement is awaiting review (current status: " + statement.getStatus() + ").",
                    List.of());
        }
        return statement;
    }

    /** Rows nobody has touched that still have no category. */
    private static List<StatementTransaction> eligible(List<StatementTransaction> rows) {
        return rows.stream()
                .filter(row -> row.getReviewStatus() == ReviewStatus.PENDING && row.getSuggestedCategoryId() == null)
                .toList();
    }

    /** The cleaned description the user sees, falling back to the raw one, with anything identifying removed. */
    private static String textFor(StatementTransaction row) {
        String text = row.getNormalizedDescription() != null && !row.getNormalizedDescription().isBlank()
                ? row.getNormalizedDescription() : row.getRawDescription();
        return PiiRedactor.redact(text);
    }

    private static boolean isDebit(StatementTransaction row) {
        return row.getSuggestedTransactionType().isUnknown() || row.getSuggestedTransactionType().isDebitSide();
    }

    private static CategoryType expectedKind(StatementTransaction row) {
        return isDebit(row) ? CategoryType.EXPENSE : CategoryType.INCOME;
    }
}
