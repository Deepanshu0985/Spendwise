package com.finance.statement;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.infrastructure.pdf.BobStatementParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BobStatementParserTest {

    private final BobStatementParser parser = new BobStatementParser();

    @Test
    void matchesTextContainingBankOfBarodaHeaderAndAnActualRow() {
        assertThat(parser.matches(
                "Bank of Baroda Statement of Account\n15-09-2026 REF001 ATM WDL CASH 5000.00 0.00 45000.00"))
                .isTrue();
        assertThat(parser.matches("HDFC BANK\nStatement")).isFalse();
    }

    @Test
    void doesNotFalseMatchOnBareBobSubstringInOtherBanksNarration() {
        assertThat(parser.matches("AXIS BANK\n15-09-2026 REF001 NEFT TO BOB ACCOUNT 500.00 0.00 4500.00")).isFalse();
    }

    @Test
    void doesNotFalseMatchWhenBankOfBarodaIsOnlyMentionedAsALinkedAccountName() {
        // The exact real-world collision found against a real Paytm statement: Paytm's
        // own passbook labels a transaction's linked account "Bank Of Baroda" without
        // any six-column row ever appearing, since Paytm's format is multi-line blocks,
        // not tabular rows - matches() must not be fooled by the name alone.
        String paytmStyleText = """
                Paytm Statement for
                Passbook Payments History
                29 Sep
                10:05 PM
                Paid to Test Merchant
                UPI Ref No: 615023242614
                 Tag:
                # Groceries
                Bank Of
                Baroda - 21
                - Rs.110
                """;

        assertThat(parser.matches(paytmStyleText)).isFalse();
    }

    @Test
    void parsesDebitAndCreditRows() {
        String text = """
                Bank of Baroda Statement of Account
                15-09-2026 REF001 ATM WDL CASH 5000.00 0.00 45000.00
                18-09-2026 REF999 SALARY CREDIT ACME 0.00 60000.00 105000.00
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
