package com.finance.statement;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.infrastructure.pdf.AxisBankStatementParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AxisBankStatementParserTest {

    private final AxisBankStatementParser parser = new AxisBankStatementParser();

    @Test
    void matchesTextContainingAxisBankHeader() {
        assertThat(parser.matches("AXIS BANK\nStatement of Account\n15-09-2026 REF001 ATM WDL CASH 5000.00 0.00 45000.00")).isTrue();
        assertThat(parser.matches("Paytm Statement\nPaid to Axis Bank Limited")).isFalse();
        assertThat(parser.matches("HDFC BANK\nStatement")).isFalse();
    }

    @Test
    void parsesDebitAndCreditRows() {
        String text = """
                AXIS BANK Statement of Account
                15-09-2026 CHQ001 AMAZON PURCHASE 1200.00 0.00 8800.00
                20-09-2026 CHQ002 FREELANCE PAYMENT 0.00 15000.00 23800.00
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
