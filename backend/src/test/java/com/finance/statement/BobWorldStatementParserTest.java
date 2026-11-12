package com.finance.statement;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.domain.statement.ParsedTransactionRow.DebitCredit;
import com.finance.infrastructure.pdf.BobStatementParser;
import com.finance.infrastructure.pdf.BobWorldStatementParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The statement from the Bank of Baroda mobile app, a different layout from the older net-banking export. The sample is
 * synthetic (invented names and figures) but follows the line order PDFBox actually extracted from a real statement.
 */
class BobWorldStatementParserTest {

    private final BobWorldStatementParser parser = new BobWorldStatementParser();

    private static String sample() throws IOException {
        try (var in = BobWorldStatementParserTest.class.getResourceAsStream("/statements/samples/BOB_WORLD.txt")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesEveryTransactionAndSkipsHeadingsFootersAndTheOpeningBalance() throws IOException {
        ParsedStatement statement = parser.parse(sample());
        assertThat(statement.rows()).hasSize(11);
        assertThat(statement.detectedBank()).isEqualTo("BOB_WORLD");
        assertThat(statement.periodStart()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(statement.periodEnd()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void readsDirectionFromTheDebitAndCreditColumnsAndTheBalanceAlwaysAgrees() throws IOException {
        ParsedStatement statement = parser.parse(sample());
        BigDecimal balance = new BigDecimal("5000.00"); // the sample's opening balance
        for (ParsedTransactionRow row : statement.rows()) {
            balance = row.debitCredit() == DebitCredit.DEBIT ? balance.subtract(row.amount()) : balance.add(row.amount());
            assertThat(row.balance()).as("running balance after %s", row.sourceRowReference()).isEqualByComparingTo(balance);
        }
        assertThat(statement.rows().get(0).debitCredit()).isEqualTo(DebitCredit.DEBIT);
        assertThat(statement.rows().get(1).debitCredit()).isEqualTo(DebitCredit.CREDIT);
    }

    @Test
    void splitsTheBalanceFromTheRunTogetherSerialNumber() throws IOException {
        List<ParsedTransactionRow> rows = parser.parse(sample()).rows();
        assertThat(rows.get(0).balance()).isEqualByComparingTo("4900.00");
        assertThat(rows.get(0).sourceRowReference()).isEqualTo("sr1");
        // two-digit serial numbers: "11,274.005" is balance 11,274.00 and Sr.No 5, but "9,012.3411" is balance 9,012.34 and Sr.No 11
        assertThat(rows.get(10).sourceRowReference()).isEqualTo("sr11");
        assertThat(rows.get(10).balance()).isEqualByComparingTo("9012.34");
    }

    @Test
    void joinsANarrationThatWrapsMidWordAndKeepsARealWordBreak() throws IOException {
        List<ParsedTransactionRow> rows = parser.parse(sample()).rows();
        assertThat(rows.get(1).rawDescription()).isEqualTo("UPI/200000000002/11:15:30/UPI/9999900000@ptyes/NA");
        assertThat(rows.get(2).rawDescription()).isEqualTo("NEFT-AXODH0000000000-EXAMPLE REVENUE MANAGERS INDIA P");
    }

    @Test
    void readsAShortNarrationThatSharesALineWithTheDatesAndTheAmounts() throws IOException {
        ParsedTransactionRow row = parser.parse(sample()).rows().get(3);
        assertThat(row.rawDescription()).isEqualTo("ACHDR/EMIDUE/1234567890/111397544160");
        assertThat(row.amount()).isEqualByComparingTo("5556.00");
        assertThat(row.debitCredit()).isEqualTo(DebitCredit.DEBIT);
        assertThat(row.transactionDate()).isEqualTo(LocalDate.of(2026, 10, 6));
    }

    @Test
    void takesTheUpiReferenceNumberForDuplicateMatching() throws IOException {
        List<ParsedTransactionRow> rows = parser.parse(sample()).rows();
        assertThat(rows.get(0).reference()).isEqualTo("200000000001");
        assertThat(rows.get(3).reference()).isNull();
    }

    @Test
    void keepsParsingAcrossAPageBreakWithItsRepeatedHeadings() throws IOException {
        // the sample has a page break after the sixth transaction; the seventh must still arrive intact
        List<ParsedTransactionRow> rows = parser.parse(sample()).rows();
        assertThat(rows.get(6).rawDescription()).isEqualTo("UPI/200000000005/08:00:00/UPI/airtelprepaidUPWest");
        assertThat(rows.get(6).amount()).isEqualByComparingTo("299.00");
    }

    @Test
    void recognisesItsOwnLayoutButNotTheOlderBobExportNorAnotherBanks() throws IOException {
        assertThat(parser.matches(sample())).isTrue();

        String olderBobExport = """
                https://www.bankofbaroda.bank.in Customer Care
                01-07-2026
                UPI/209737101589/14:14:17/UPI/test1234@
                okbank/NA
                15000.00 15078.62 Cr
                """;
        assertThat(parser.matches(olderBobExport)).isFalse();
        assertThat(new BobStatementParser().matches(sample())).as("the older reader must not claim the new layout").isFalse();

        // a narration that merely names the bank, in a layout that is not this one
        assertThat(parser.matches("Account Statement from 01-10-2026 to 31-10-2026\nPaid to Bank of Baroda Ltd 100.00 -")).isFalse();
    }

    @Test
    void anUnfinishedBlockIsDroppedRatherThanGuessed() {
        String text = """
                Account Statement from 01-10-2026 to 31-10-2026
                4,900.001 02-10-2026 02-10-2026
                UPI/200000000001/10:00:00/UPI/shopone@ptybl/Sent u
                5,150.002 03-10-2026 03-10-2026
                UPI/200000000002/11:15:30/UPI/someone@ptyes/NA
                - 250.00
                """;
        List<ParsedTransactionRow> rows = new ArrayList<>(parser.parse(text).rows());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).amount()).isEqualByComparingTo("250.00");
    }
}
