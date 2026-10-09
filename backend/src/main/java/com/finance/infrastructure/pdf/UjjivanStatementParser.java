package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Ujjivan Small Finance Bank's statement layout: Date | Ref No | Description |
 * Debit | Credit | Balance - the same six-column shape as Axis Bank, BOB and
 * Paytm Wallet (see StatementParsingSupport.parseSixColumnRows).
 */
@Component
public class UjjivanStatementParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // date  refNo  description (non-greedy)  debit  credit  balance
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "^(\\d{2}-\\d{2}-\\d{4})\\s+(\\S+)\\s+(.+?)\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s*$");

    @Override
    public String bankName() {
        return "UJJIVAN";
    }

    @Override
    public String displayName() {
        return "Ujjivan Small Finance Bank";
    }

    @Override
    public int identityPosition(String extractedText) {
        return extractedText.toUpperCase().indexOf("UJJIVAN");
    }

    @Override
    public boolean matches(String extractedText) {
        // The name alone isn't enough: Paytm's own passbook statement labels a row's
        // linked account "Ujjivan Small Finance Bank" without being a Ujjivan statement
        // itself, so an actual six-column row must be present too (found against a real
        // Paytm file - see StatementParsingSupport.hasMatchingRow).
        return extractedText.toUpperCase().contains("UJJIVAN")
                && StatementParsingSupport.hasMatchingRow(extractedText, ROW_PATTERN);
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        return StatementParsingSupport.parseSixColumnRows(extractedText, bankName(), ROW_PATTERN, DATE_FORMAT);
    }
}
