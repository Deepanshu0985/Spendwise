package com.finance.domain.ai;

import com.finance.domain.category.Category;
import com.finance.domain.category.CategoryType;
import com.finance.domain.transaction.TransactionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Nothing a model says is used until it passes here (prompt-design.md: schema validation, and a response outside the
 * ledger-side types falls back to the review queue). Anything doubtful is dropped, never repaired: the row simply stays
 * for the user to review.
 */
public final class AiSuggestionValidator {

    /** Types a model may choose: those a statement row can really be without needing a transfer partner or card account. */
    static final Set<TransactionType> ALLOWED_TYPES = Set.of(
            TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.REFUND, TransactionType.FEE_CHARGED,
            TransactionType.INTEREST_CHARGED, TransactionType.INTEREST_EARNED);

    private static final int MAX_REASON_LENGTH = 200;
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");

    private AiSuggestionValidator() {
    }

    /**
     * @param rows       the rows that were asked about, in the order they were numbered for the model (index 1 is rows.get(0))
     * @param categories the user's categories the model was allowed to choose from
     */
    public static List<AiSuggestion> validate(List<AiRowInput> rows, List<RawSuggestion> raw, List<Category> categories) {
        Map<String, Category> byName = categories.stream()
                .collect(Collectors.toMap(c -> normalise(c.getName()), c -> c, (first, second) -> first));
        Set<Integer> seen = new HashSet<>();
        List<AiSuggestion> accepted = new ArrayList<>();
        for (RawSuggestion suggestion : raw) {
            Integer index = suggestion.index();
            if (index == null || index < 1 || index > rows.size() || !seen.add(index)) {
                continue;
            }
            AiRowInput row = rows.get(index - 1);
            TransactionType type = parseType(suggestion.transactionType());
            Category category = suggestion.category() == null ? null : byName.get(normalise(suggestion.category()));
            Double confidence = suggestion.confidence();
            if (type == null || !ALLOWED_TYPES.contains(type) || category == null || confidence == null
                    || confidence.isNaN() || confidence < 0 || confidence > 1) {
                continue;
            }
            // The model cannot turn a payment out into income or the reverse, nor file it under the wrong kind of category.
            boolean typeFitsDirection = type.isDebitSide() == row.debit();
            boolean categoryFitsType = category.getCategoryType() == (type.isDebitSide() ? CategoryType.EXPENSE : CategoryType.INCOME);
            if (!typeFitsDirection || !categoryFitsType) {
                continue;
            }
            accepted.add(new AiSuggestion(row.rowId(), type, category.getId(), confidence, cleanReason(suggestion.reason())));
        }
        return accepted;
    }

    private static TransactionType parseType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TransactionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String normalise(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static String cleanReason(String reason) {
        if (reason == null) {
            return null;
        }
        String cleaned = CONTROL_CHARS.matcher(reason).replaceAll(" ").trim();
        return cleaned.length() > MAX_REASON_LENGTH ? cleaned.substring(0, MAX_REASON_LENGTH) : cleaned;
    }
}
