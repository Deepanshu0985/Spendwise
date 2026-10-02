package com.finance.statement;

import com.finance.domain.statement.PayeeKey;
import com.finance.domain.statement.StatementTransaction;
import com.finance.domain.transaction.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PayeeRuleTest {

    private StatementTransaction row(String description) {
        return new StatementTransaction(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 9, 1), BigDecimal.TEN, "INR", description, description,
                null, null, TransactionType.EXPENSE, new BigDecimal("0.60"), "p1r1", null);
    }

    @Test
    void keyIgnoresCaseAndPunctuationButKeepsTheWords() {
        assertThat(PayeeKey.of("UPI: Zomato")).isEqualTo("upi zomato");
        assertThat(PayeeKey.of("  Paid to  Rana Hyperstore, The Supermarket ")).isEqualTo("paid to rana hyperstore the supermarket");
        assertThat(PayeeKey.of(null)).isEmpty();
    }

    @Test
    void aRemembersChoiceFillsOnlyWhatTheRowDoesNotAlreadyHave() {
        UUID merchant = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        StatementTransaction untouched = row("UPI: Zomato");

        assertThat(untouched.applySuggestion(merchant, category)).isTrue();
        assertThat(untouched.getSuggestedMerchantId()).isEqualTo(merchant);
        assertThat(untouched.getSuggestedCategoryId()).isEqualTo(category);

        UUID other = UUID.randomUUID();
        assertThat(untouched.applySuggestion(other, other)).isFalse();
        assertThat(untouched.getSuggestedCategoryId()).isEqualTo(category);
    }

    @Test
    void aRowTheUserAlreadyReviewedIsNeverOverwritten() {
        StatementTransaction reviewed = row("UPI: Zomato");
        UUID chosen = UUID.randomUUID();
        reviewed.applyReview(reviewed.getTransactionDate(), reviewed.getAmount(), null, chosen, TransactionType.EXPENSE);

        assertThat(reviewed.applySuggestion(UUID.randomUUID(), UUID.randomUUID())).isFalse();
        assertThat(reviewed.getSuggestedCategoryId()).isEqualTo(chosen);
    }
}
