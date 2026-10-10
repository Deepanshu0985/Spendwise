package com.finance.ai;

import com.finance.domain.ai.AiRowInput;
import com.finance.domain.ai.AiSuggestion;
import com.finance.domain.ai.AiSuggestionValidator;
import com.finance.domain.ai.RawSuggestion;
import com.finance.domain.category.Category;
import com.finance.domain.category.CategoryType;
import com.finance.domain.transaction.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiSuggestionValidatorTest {

    private static Category category(String name, CategoryType type) {
        return new Category(UUID.randomUUID(), null, null, name, type, true, true, Instant.now(), Instant.now());
    }

    private final Category food = category("Food & Dining", CategoryType.EXPENSE);
    private final Category salary = category("Salary", CategoryType.INCOME);
    private final List<Category> categories = List.of(food, salary);
    private final AiRowInput debitRow = new AiRowInput(UUID.randomUUID(), "Paid to Cafe", true);
    private final AiRowInput creditRow = new AiRowInput(UUID.randomUUID(), "ACME payroll", false);
    private final List<AiRowInput> rows = List.of(debitRow, creditRow);

    private static RawSuggestion raw(Integer index, String type, String category, Double confidence, String reason) {
        return new RawSuggestion(index, type, category, confidence, reason);
    }

    @Test
    void aWellFormedSuggestionForARealRowAndCategoryIsAccepted() {
        List<AiSuggestion> result = AiSuggestionValidator.validate(rows,
                List.of(raw(1, "EXPENSE", "food & dining ", 0.9, "A cafe"), raw(2, "INCOME", "Salary", 0.95, "Payroll")), categories);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).rowId()).isEqualTo(debitRow.rowId());
        assertThat(result.get(0).categoryId()).isEqualTo(food.getId());
        assertThat(result.get(0).type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.get(1).categoryId()).isEqualTo(salary.getId());
    }

    @Test
    void aCategoryTheUserDoesNotHaveIsDroppedNotInvented() {
        assertThat(AiSuggestionValidator.validate(rows, List.of(raw(1, "EXPENSE", "Crypto Gambling", 0.99, "x")), categories)).isEmpty();
        assertThat(AiSuggestionValidator.validate(rows, List.of(raw(1, "EXPENSE", null, 0.99, "unsure")), categories)).isEmpty();
    }

    @Test
    void aTypeOutsideTheAllowedLedgerSetIsDropped() {
        assertThat(AiSuggestionValidator.validate(rows, List.of(raw(1, "TRANSFER_OUT", "Food & Dining", 0.9, "x")), categories)).isEmpty();
        assertThat(AiSuggestionValidator.validate(rows, List.of(raw(1, "UNKNOWN", "Food & Dining", 0.9, "x")), categories)).isEmpty();
        assertThat(AiSuggestionValidator.validate(rows, List.of(raw(1, "DROP TABLE", "Food & Dining", 0.9, "x")), categories)).isEmpty();
        assertThat(AiSuggestionValidator.validate(rows, List.of(raw(1, null, "Food & Dining", 0.9, "x")), categories)).isEmpty();
    }

    @Test
    void theModelCannotFlipTheDirectionOfAPaymentOrFileItUnderTheWrongKindOfCategory() {
        // a debit row called INCOME, a credit row called EXPENSE, an expense filed under an income category
        assertThat(AiSuggestionValidator.validate(rows, List.of(raw(1, "INCOME", "Salary", 0.9, "x")), categories)).isEmpty();
        assertThat(AiSuggestionValidator.validate(rows, List.of(raw(2, "EXPENSE", "Food & Dining", 0.9, "x")), categories)).isEmpty();
        assertThat(AiSuggestionValidator.validate(rows, List.of(raw(1, "EXPENSE", "Salary", 0.9, "x")), categories)).isEmpty();
    }

    @Test
    void badIndexesAndConfidencesAreDropped() {
        List<RawSuggestion> raws = List.of(
                raw(0, "EXPENSE", "Food & Dining", 0.9, "x"), raw(3, "EXPENSE", "Food & Dining", 0.9, "x"),
                raw(null, "EXPENSE", "Food & Dining", 0.9, "x"));
        assertThat(AiSuggestionValidator.validate(rows, raws, categories)).isEmpty();

        for (Double badConfidence : new Double[] {1.5, -0.1, Double.NaN, null}) {
            assertThat(AiSuggestionValidator.validate(rows, List.of(raw(1, "EXPENSE", "Food & Dining", badConfidence, "x")), categories))
                    .as("confidence %s", badConfidence).isEmpty();
        }
    }

    @Test
    void whenTheModelAnswersTheSameRowTwiceTheFirstAnswerIsTheOnlyOneConsidered() {
        // A model that contradicts itself is not trusted: a valid second answer does not rescue an invalid first one.
        List<AiSuggestion> firstValid = AiSuggestionValidator.validate(rows,
                List.of(raw(1, "EXPENSE", "Food & Dining", 0.8, "first"), raw(1, "EXPENSE", "Food & Dining", 0.9, "second")), categories);
        assertThat(firstValid).hasSize(1);
        assertThat(firstValid.get(0).reason()).isEqualTo("first");

        assertThat(AiSuggestionValidator.validate(rows,
                List.of(raw(1, "EXPENSE", "Food & Dining", 1.5, "invalid first"), raw(1, "EXPENSE", "Food & Dining", 0.9, "valid second")),
                categories)).isEmpty();
    }

    @Test
    void theReasonIsCleanedAndCapped() {
        List<AiSuggestion> result = AiSuggestionValidator.validate(rows,
                List.of(raw(1, "EXPENSE", "Food & Dining", 0.9, "line1\nline2" + "y".repeat(400))), categories);

        assertThat(result.get(0).reason()).doesNotContain("\n").hasSize(200);
    }
}
