package com.finance.statement;

import com.finance.domain.statement.DuplicateMatch;
import com.finance.domain.statement.DuplicateReason;
import com.finance.domain.statement.DuplicateScorer;
import com.finance.domain.statement.DuplicateStatus;
import com.finance.domain.statement.StatementTransaction;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateScorerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID bank = UUID.randomUUID();
    private final UUID wallet = UUID.randomUUID();

    private StatementTransaction staged(String date, String amount, String raw, String reference) {
        return new StatementTransaction(
                userId, UUID.randomUUID(), LocalDate.parse(date), new BigDecimal(amount), "INR", raw, raw, null, null,
                TransactionType.EXPENSE, new BigDecimal("0.60"), "p1r1", reference);
    }

    private Transaction imported(UUID account, String date, String amount, String raw, String reference) {
        return new Transaction(
                userId, account, null, null, LocalDate.parse(date), new BigDecimal(amount), "INR", raw, raw,
                TransactionType.EXPENSE, TransactionSource.STATEMENT, reference, TransactionStatus.CONFIRMED);
    }

    @Test
    void exactReferenceIsADuplicateEvenInAnotherAccount() {
        // The same UPI payment appears in both the wallet statement and the bank statement it was paid from.
        Transaction fromBank = imported(bank, "2026-09-24", "430.00", "UPI/216429284872/12:27:41/UPI/7302271342@ptyes/NA", "216429284872");
        StatementTransaction walletRow = staged("2026-09-24", "430", "Money sent to Priyanshu", "216429284872");

        DuplicateMatch match = DuplicateScorer.score(walletRow, wallet, List.of(fromBank), new HashSet<>()).orElseThrow();

        assertThat(match.status()).isEqualTo(DuplicateStatus.DUPLICATE);
        assertThat(match.reason()).isEqualTo(DuplicateReason.EXACT_REFERENCE);
        assertThat(match.matchedTransactionId()).isEqualTo(fromBank.getId());
    }

    @Test
    void sameReferenceButDifferentAmountIsNotADuplicate() {
        Transaction existing = imported(bank, "2026-09-24", "430.00", "UPI/1/x", "216429284872");
        StatementTransaction row = staged("2026-09-24", "431", "UPI/1/x", "216429284872");

        assertThat(DuplicateScorer.score(row, bank, List.of(existing), new HashSet<>())).isEmpty();
    }

    @Test
    void sameAccountSameDayAmountAndDescriptionIsADuplicateWithoutAReference() {
        Transaction existing = imported(bank, "2026-09-15", "500.00", "SWIGGY BANGALORE", null);
        StatementTransaction row = staged("2026-09-15", "500", "SWIGGY BANGALORE", null);

        DuplicateMatch match = DuplicateScorer.score(row, bank, List.of(existing), new HashSet<>()).orElseThrow();

        assertThat(match.status()).isEqualTo(DuplicateStatus.DUPLICATE);
        assertThat(match.reason()).isEqualTo(DuplicateReason.DATE_AMOUNT_DESCRIPTION);
    }

    @Test
    void nearbyDateWithSimilarDescriptionIsOnlyAPossibleDuplicate() {
        Transaction existing = imported(bank, "2026-09-15", "500.00", "SWIGGY BANGALORE", null);
        StatementTransaction row = staged("2026-09-16", "500", "SWIGGY BANGALORE", null);

        DuplicateMatch match = DuplicateScorer.score(row, bank, List.of(existing), new HashSet<>()).orElseThrow();

        assertThat(match.status()).isEqualTo(DuplicateStatus.POSSIBLE_DUPLICATE);
        assertThat(match.reason()).isEqualTo(DuplicateReason.NEARBY_SIMILAR);
    }

    @Test
    void differentMerchantOnTheSameDayAndAmountIsNotADuplicate() {
        Transaction existing = imported(bank, "2026-09-15", "500.00", "SWIGGY BANGALORE", null);
        StatementTransaction row = staged("2026-09-15", "500", "AMAZON PAY INDIA", null);

        assertThat(DuplicateScorer.score(row, bank, List.of(existing), new HashSet<>())).isEmpty();
    }

    @Test
    void anotherAccountsMatchingDescriptionDoesNotCountWithoutAReference() {
        Transaction existing = imported(bank, "2026-09-15", "500.00", "SWIGGY BANGALORE", null);
        StatementTransaction row = staged("2026-09-15", "500", "SWIGGY BANGALORE", null);

        assertThat(DuplicateScorer.score(row, wallet, List.of(existing), new HashSet<>())).isEmpty();
    }

    @Test
    void anImportedTransactionIsOnlyConsumedByOneStagedRow() {
        Transaction existing = imported(bank, "2026-09-15", "50.00", "MINTU TEA STALL", null);
        StatementTransaction first = staged("2026-09-15", "50", "MINTU TEA STALL", null);
        StatementTransaction second = staged("2026-09-15", "50", "MINTU TEA STALL", null);
        HashSet<UUID> matched = new HashSet<>();

        DuplicateMatch firstMatch = DuplicateScorer.score(first, bank, List.of(existing), matched).orElseThrow();
        matched.add(firstMatch.matchedTransactionId());

        assertThat(DuplicateScorer.score(second, bank, List.of(existing), matched)).isEmpty();
    }

    @Test
    void aUserOverrideSurvivesRescoring() {
        Transaction existing = imported(bank, "2026-09-15", "500.00", "SWIGGY BANGALORE", null);
        StatementTransaction row = staged("2026-09-15", "500", "SWIGGY BANGALORE", null);
        row.applyDuplicateScore(DuplicateScorer.score(row, bank, List.of(existing), new HashSet<>()).orElseThrow());
        assertThat(row.isUnresolvedDuplicate()).isTrue();

        row.overrideDuplicate(Instant.now());
        row.applyDuplicateScore(DuplicateScorer.score(row, bank, List.of(existing), new HashSet<>()).orElseThrow());

        assertThat(row.isUnresolvedDuplicate()).isFalse();
        assertThat(row.getDuplicateStatus()).isEqualTo(DuplicateStatus.NOT_DUPLICATE);
        assertThat(row.getDuplicateOverriddenAt()).isNotNull();
    }
}
