package com.finance.statement;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.infrastructure.pdf.SbiStatementParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SbiStatementParserTest {

    private final SbiStatementParser parser = new SbiStatementParser();

    @Test
    void matchesTextContainingStateBankOfIndiaHeader() {
        assertThat(parser.matches("State Bank of India\nStatement of Account\n15-09-2026 15-09-2026 ATM WDL CASH REF001 5000.00 0.00 45000.00")).isTrue();
        assertThat(parser.matches("Paytm Statement\nPaid to State Bank of India")).isFalse();
        assertThat(parser.matches("HDFC BANK\nStatement")).isFalse();
    }

    @Test
    void doesNotFalseMatchOnBareSbiSubstringInOtherBanksNarration() {
        // Regression: an earlier version matched the bare "SBI" substring, which could
        // false-positive on narration text like "NEFT TO SBI A/C" inside another bank's statement.
        assertThat(parser.matches("HDFC BANK\n15/09/26 NEFT TO SBI ACCOUNT REF001 15/09/26 500.00 0.00 4500.00")).isFalse();
    }

    @Test
    void parsesDebitAndCreditRows() {
        String text = """
                State Bank of India Statement of Account
                15-09-2026 15-09-2026 ATM WDL CASH REF001 5000.00 0.00 45000.00
                18-09-2026 18-09-2026 SALARY CREDIT ACME REF999 0.00 60000.00 105000.00
                """;

        ParsedStatement result = parser.parse(text);

        assertThat(result.rows()).hasSize(2);
        ParsedTransactionRow debit = result.rows().get(0);
        assertThat(debit.transactionDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(debit.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.DEBIT);
        assertThat(debit.amount()).isEqualByComparingTo("5000.00");

        ParsedTransactionRow credit = result.rows().get(1);
        assertThat(credit.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.CREDIT);
        assertThat(credit.amount()).isEqualByComparingTo("60000.00");
    }
}
