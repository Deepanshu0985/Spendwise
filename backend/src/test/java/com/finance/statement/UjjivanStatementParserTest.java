package com.finance.statement;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.infrastructure.pdf.UjjivanStatementParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UjjivanStatementParserTest {

    private final UjjivanStatementParser parser = new UjjivanStatementParser();

    @Test
    void matchesTextContainingUjjivanHeader() {
        assertThat(parser.matches("Ujjivan Small Finance Bank\nStatement of Account")).isTrue();
        assertThat(parser.matches("HDFC BANK\nStatement")).isFalse();
    }

    @Test
    void parsesDebitAndCreditRows() {
        String text = """
                Ujjivan Small Finance Bank Statement of Account
                15-09-2026 REF001 AMAZON PURCHASE 1200.00 0.00 8800.00
                20-09-2026 REF002 FREELANCE PAYMENT 0.00 15000.00 23800.00
                """;

        ParsedStatement result = parser.parse(text);

        assertThat(result.rows()).hasSize(2);
        ParsedTransactionRow debit = result.rows().get(0);
        assertThat(debit.transactionDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(debit.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.DEBIT);
        assertThat(debit.rawDescription()).isEqualTo("AMAZON PURCHASE");
        assertThat(debit.amount()).isEqualByComparingTo("1200.00");

        ParsedTransactionRow credit = result.rows().get(1);
        assertThat(credit.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.CREDIT);
        assertThat(credit.amount()).isEqualByComparingTo("15000.00");
    }
}
