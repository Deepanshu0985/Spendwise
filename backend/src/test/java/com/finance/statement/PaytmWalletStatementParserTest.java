package com.finance.statement;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.infrastructure.pdf.PaytmWalletStatementParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaytmWalletStatementParserTest {

    private final PaytmWalletStatementParser parser = new PaytmWalletStatementParser();

    @Test
    void matchesTextContainingThePassbookPaymentsHistoryHeader() {
        assertThat(parser.matches("Paytm Statement for\nPassbook Payments History")).isTrue();
        assertThat(parser.matches("HDFC BANK\nStatement")).isFalse();
    }

    @Test
    void doesNotFalseMatchWhenPaytmIsOnlyMentionedAsAUpiHandleInAnotherBanksNarration() {
        // The exact real-world collision found against a real Bank of Baroda statement:
        // BOB's own UPI narrations routinely reference Paytm-linked merchant handles
        // ("UPI/.../paytm.d11487.../...", "paytmqr...") without the statement being a
        // Paytm export at all - a bare "PAYTM" substring check used to false-match this
        // and win the format-detection race before BobStatementParser was ever tried.
        String bobStyleText = """
                Statement of transactions in Savings Account 12345678901 in INR
                https://www.bankofbaroda.bank.in Customer Care
                10-07-2026
                UPI/310153745613/22:30:08/UPI/paytm.d11487
                868129@
                4000.00 21983.43 Cr
                """;

        assertThat(parser.matches(bobStyleText)).isFalse();
    }

    /**
     * Shaped after PDFBox's actual line-by-line extraction of a real Paytm "Passbook
     * Payments History" export (verified against the user's own statement, not guessed):
     * one multi-line block per transaction, a wrapped description, an optional "UPI ID:"
     * line, an always-present "UPI Ref No:" line, "Tag:" then "# Category", then the
     * linked account name and the signed amount. A short account name like "UPI Lite"
     * doesn't wrap, so its line ends up combined with the amount on one line instead of
     * two - the parser must find the amount at the END of that line, not require the
     * whole line to be just the amount.
     */
    private static final String STATEMENT_TEXT = """
            Paytm Statement for
            30 AUG'26 - 29 SEP'26
            Passbook Payments History
            29 Sep
            10:05 PM
            Paid to Test Grocery And
            Store
            UPI ID: test-11259061333@okbizaxis
            UPI Ref No: 615023242614
             Tag:
            # Groceries
            Ujjivan Small
            Finance
            Bank - 82
            - Rs.110
            28 Sep
            12:44 PM
            Paid to Test Cafe
            UPI ID: test-11244387961@okbizaxis
            UPI Ref No: 216815884724
             Tag:
            # Food
            UPI Lite - Rs.30
            27 Sep
            9:47 PM
            Received from Test Sender
            UPI Ref No: 216709543148
             Tag:
            # Transfer
            UPI Lite + Rs.500
            23 Sep
            6:22 PM
            Automatic Add Money for UPI Lite
            UPI Ref No: 663200437791
            Note: Topup with
            Rs 1000
             Tag:
            # Self-Transfer
            Bank Of
            Baroda - 21
            Rs.1,000
            Note: This payment is not included in the total money paid and money received calculations.
            """;

    @Test
    void parsesDebitAndCreditRowsFromRealPassbookFormat() {
        ParsedStatement result = parser.parse(STATEMENT_TEXT);

        assertThat(result.rows()).hasSize(3);

        ParsedTransactionRow wrappedAccountRow = result.rows().get(0);
        assertThat(wrappedAccountRow.transactionDate()).isEqualTo(LocalDate.of(2026, 9, 29));
        assertThat(wrappedAccountRow.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.DEBIT);
        assertThat(wrappedAccountRow.rawDescription()).isEqualTo("Paid to Test Grocery And Store");
        assertThat(wrappedAccountRow.amount()).isEqualByComparingTo("110");

        ParsedTransactionRow inlineAccountRow = result.rows().get(1);
        assertThat(inlineAccountRow.transactionDate()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(inlineAccountRow.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.DEBIT);
        assertThat(inlineAccountRow.rawDescription()).isEqualTo("Paid to Test Cafe");
        assertThat(inlineAccountRow.amount()).isEqualByComparingTo("30");

        ParsedTransactionRow creditRow = result.rows().get(2);
        assertThat(creditRow.transactionDate()).isEqualTo(LocalDate.of(2026, 9, 27));
        assertThat(creditRow.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.CREDIT);
        assertThat(creditRow.amount()).isEqualByComparingTo("500");
    }

    @Test
    void capturesThePrintedFundingAccountOfEachRow() {
        ParsedStatement result = parser.parse(STATEMENT_TEXT);

        // wrapped over several lines, short enough to share the amount line, and a wallet-style label
        assertThat(result.rows().get(0).accountLabel()).isEqualTo("Ujjivan Small Finance Bank - 82");
        assertThat(result.rows().get(1).accountLabel()).isEqualTo("UPI Lite");
        assertThat(result.rows().get(2).accountLabel()).isEqualTo("UPI Lite");
    }

    @Test
    void excludesSelfTransfersFromStagedRows() {
        ParsedStatement result = parser.parse(STATEMENT_TEXT);

        assertThat(result.rows())
                .noneMatch(row -> row.rawDescription().contains("Automatic Add Money"));
    }

    @Test
    void derivesPeriodFromStatementHeader() {
        ParsedStatement result = parser.parse(STATEMENT_TEXT);

        assertThat(result.periodStart()).isEqualTo(LocalDate.of(2026, 9, 27));
        assertThat(result.periodEnd()).isEqualTo(LocalDate.of(2026, 9, 29));
    }
}
