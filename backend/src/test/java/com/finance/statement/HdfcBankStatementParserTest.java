package com.finance.statement;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.infrastructure.pdf.HdfcBankStatementParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HdfcBankStatementParserTest {

    private final HdfcBankStatementParser parser = new HdfcBankStatementParser();

    @Test
    void matchesTextContainingHdfcBankHeader() {
        assertThat(parser.matches("HDFC BANK\nStatement of Account\n15/09/26 SWIGGY BANGALORE UPI123456 15/09/26 500.00 0.00 4500.00")).isTrue();
        assertThat(parser.matches("Paytm Statement\nPaid to HDFC Bank Limited")).isFalse();
        assertThat(parser.matches("State Bank of India\nStatement")).isFalse();
    }

    @Test
    void parsesDebitAndCreditRows() {
        String text = """
                HDFC BANK Statement of Account
                15/09/26 SWIGGY BANGALORE UPI123456 15/09/26 500.00 0.00 4500.00
                18/09/26 SALARY CREDIT ACME CORP NEFT999 18/09/26 0.00 50000.00 54500.00
                """;

        ParsedStatement result = parser.parse(text);

        assertThat(result.rows()).hasSize(2);

        ParsedTransactionRow debit = result.rows().get(0);
        assertThat(debit.transactionDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(debit.amount()).isEqualByComparingTo("500.00");
        assertThat(debit.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.DEBIT);
        assertThat(debit.rawDescription()).isEqualTo("SWIGGY BANGALORE");
        assertThat(debit.balance()).isEqualByComparingTo("4500.00");

        ParsedTransactionRow credit = result.rows().get(1);
        assertThat(credit.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.CREDIT);
        assertThat(credit.amount()).isEqualByComparingTo("50000.00");

        assertThat(result.periodStart()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(result.periodEnd()).isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    void skipsLinesThatDoNotMatchTheRowFormat() {
        String text = """
                HDFC BANK Statement of Account
                Account Number: 1234567890
                Generated on 30/09/2026
                """;

        ParsedStatement result = parser.parse(text);

        assertThat(result.rows()).isEmpty();
    }

    @Test
    void handlesThousandsSeparatorsInAmounts() {
        String text = "HDFC BANK\n15/09/26 BIG PURCHASE STORE REF001 15/09/26 12,500.50 0.00 87,499.50";

        ParsedStatement result = parser.parse(text);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).amount()).isEqualByComparingTo(new BigDecimal("12500.50"));
        assertThat(result.rows().get(0).balance()).isEqualByComparingTo(new BigDecimal("87499.50"));
    }
}
