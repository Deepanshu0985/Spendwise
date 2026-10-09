package com.finance.statement;

import com.finance.infrastructure.pdf.AxisBankStatementParser;
import com.finance.infrastructure.pdf.BobStatementParser;
import com.finance.infrastructure.pdf.HdfcBankStatementParser;
import com.finance.infrastructure.pdf.PaytmWalletStatementParser;
import com.finance.infrastructure.pdf.SbiStatementParser;
import com.finance.infrastructure.pdf.StatementFormatDetector;
import com.finance.infrastructure.pdf.StatementParser;
import com.finance.infrastructure.pdf.UjjivanStatementParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the actual selection across every real parser, not each parser's matches() alone: a statement
 * that merely mentions another bank must still reach its own parser, whatever order Spring registers
 * the parsers in (that order depends on classpath scanning, so it differs between a laptop and a jar).
 */
class StatementFormatDetectorTest {

    private static final String PAYTM_TEXT = """
            Paytm Statement for
            03 OCT'26 - 05 OCT'26
            Ujjivan Small Finance Bank - 82 Rs.10,423.13
            Bank Of Baroda - 21 Rs.2,210
            Passbook Payments History
            05 Oct
            10:05 PM
            Paid to Axis Bank Limited
            Paid to HDFC Bank Ltd and State Bank of India
            UPI Ref No: 615023242614
             Tag:
            # Bills
            Ujjivan Small
            Finance
            Bank - 82
            - Rs.110
            """;

    private static final String BOB_TEXT = """
            https://www.bankofbaroda.bank.in Customer Care
            01-07-2026 Opening Balance 78.62 Cr
            01-07-2026
            UPI/209737101589/14:14:17/UPI/paytm.d11487@ptyes
            NEFT to Axis Bank, HDFC Bank, State Bank of India
            15000.00 15078.62 Cr
            """;

    private List<StatementParser> allParsers() {
        return List.of(
                new AxisBankStatementParser(), new HdfcBankStatementParser(), new SbiStatementParser(),
                new UjjivanStatementParser(), new BobStatementParser(), new PaytmWalletStatementParser());
    }

    private List<StatementParser> reversed() {
        return allParsers().reversed();
    }

    @Test
    void aPaytmStatementThatMentionsOtherBanksStillGoesToThePaytmParserInEitherOrder() {
        assertThat(new StatementFormatDetector(allParsers()).detect(PAYTM_TEXT).bankName()).isEqualTo("PAYTM_WALLET");
        assertThat(new StatementFormatDetector(reversed()).detect(PAYTM_TEXT).bankName()).isEqualTo("PAYTM_WALLET");
    }

    @Test
    void aBobStatementThatMentionsOtherBanksAndPaytmStillGoesToTheBobParserInEitherOrder() {
        assertThat(new StatementFormatDetector(allParsers()).detect(BOB_TEXT).bankName()).isEqualTo("BOB");
        assertThat(new StatementFormatDetector(reversed()).detect(BOB_TEXT).bankName()).isEqualTo("BOB");
    }
}
