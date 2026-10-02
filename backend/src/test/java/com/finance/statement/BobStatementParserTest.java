package com.finance.statement;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.infrastructure.pdf.BobStatementParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BobStatementParserTest {

    private final BobStatementParser parser = new BobStatementParser();

    /**
     * Shaped after PDFBox's actual line-by-line extraction of a real Bank of
     * Baroda statement (verified against the user's own statement, not
     * guessed): the real export never spells out "Bank of Baroda" as a
     * phrase - only the unspaced footer URL "bankofbaroda.bank.in" - and
     * carries no separate debit/credit column, only one amount plus the
     * running balance, so direction has to be inferred from the balance
     * change. Most rows wrap description across lines; a short one (the
     * ACHDR row here) fits entirely on one line instead.
     */
    private static final String STATEMENT_TEXT = """
            Statement of transactions in Savings Account 12345678901 in INR for the period Jul 01, 2026 - Sep 29, 2026
            https://www.bankofbaroda.bank.in Customer Care
            DATE NARRATION CHQ.NO. WITHDRAWAL (DR) DEPOSIT (CR) BALANCE
            01-07-2026 Opening Balance 78.62 Cr
            01-07-2026
            UPI/209737101589/14:14:17/UPI/test1234@
            okbank/NA
            15000.00 15078.62 Cr
            02-07-2026
            UPI/609609496741/18:05:55/UPI/testmerchant.r
            zp@hdf
            283.08 14795.54 Cr
            05-07-2026 ACHDR/EMIDUE/1234567890/111397544160 5556.00 9239.46 Cr
            29-09-2026 Closing Balance 9239.46 Cr
            """;

    @Test
    void matchesTextContainingTheBankOfBarodaFooterUrlAndAnActualRow() {
        assertThat(parser.matches(STATEMENT_TEXT)).isTrue();
        assertThat(parser.matches("HDFC BANK\nStatement")).isFalse();
    }

    @Test
    void doesNotFalseMatchOnBareBobSubstringInOtherBanksNarration() {
        assertThat(parser.matches("AXIS BANK\n15-09-2026 REF001 NEFT TO BOB ACCOUNT 500.00 0.00 4500.00")).isFalse();
    }

    @Test
    void doesNotFalseMatchWhenBobIsOnlyMentionedAsALinkedAccountName() {
        // The exact real-world collision found against a real Paytm statement: Paytm's
        // own passbook labels a transaction's linked account "Bank Of Baroda" (wrapped
        // across two lines) without being a BOB statement itself, and without any line
        // matching BOB's real "amount balance Cr/Dr" row shape - matches() must not be
        // fooled by the name alone.
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
    void parsesMultiLineAndSingleLineRowsInferringDirectionFromBalanceMovement() {
        ParsedStatement result = parser.parse(STATEMENT_TEXT);

        assertThat(result.rows()).hasSize(3);

        ParsedTransactionRow credit = result.rows().get(0);
        assertThat(credit.transactionDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(credit.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.CREDIT);
        assertThat(credit.amount()).isEqualByComparingTo("15000.00");
        // wrapped mid-handle ("test1234@" / "okbank/NA") joins with no space
        assertThat(credit.rawDescription()).isEqualTo("UPI/209737101589/14:14:17/UPI/test1234@okbank/NA");

        ParsedTransactionRow debit = result.rows().get(1);
        assertThat(debit.transactionDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(debit.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.DEBIT);
        assertThat(debit.amount()).isEqualByComparingTo("283.08");

        ParsedTransactionRow singleLineDebit = result.rows().get(2);
        assertThat(singleLineDebit.transactionDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(singleLineDebit.debitCredit()).isEqualTo(ParsedTransactionRow.DebitCredit.DEBIT);
        assertThat(singleLineDebit.amount()).isEqualByComparingTo("5556.00");
        assertThat(singleLineDebit.rawDescription()).isEqualTo("ACHDR/EMIDUE/1234567890/111397544160");
    }

    @Test
    void excludesOpeningAndClosingBalanceLinesFromStagedRows() {
        ParsedStatement result = parser.parse(STATEMENT_TEXT);

        assertThat(result.rows()).noneMatch(row -> row.rawDescription().contains("Balance"));
    }

    @Test
    void keepsASpaceWhereTheWrapFellOnARealWordBreak() {
        String text = """
                https://www.bankofbaroda.bank.in Customer Care
                01-07-2026 Opening Balance 100.00 Cr
                06-07-2026
                NEFT-HDFCH01106802922-PANGLOSS\s
                REIMBURSEMENT MGMT
                81945.00 82045.00 Cr
                """;

        assertThat(parser.parse(text).rows().get(0).rawDescription())
                .isEqualTo("NEFT-HDFCH01106802922-PANGLOSS REIMBURSEMENT MGMT");
    }

    @Test
    void extractsTheUpiReferenceSoItCanMatchTheSamePaymentInAnotherStatement() {
        ParsedStatement result = parser.parse(STATEMENT_TEXT);

        assertThat(result.rows().get(0).reference()).isEqualTo("209737101589");
        assertThat(result.rows().get(2).reference()).isNull();
    }
}
