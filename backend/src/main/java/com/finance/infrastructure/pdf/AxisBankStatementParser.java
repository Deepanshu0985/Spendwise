package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Axis Bank's statement layout: Tran Date | Chq No | Particulars | Debit |
 * Credit | Balance.
 */
@Component
public class AxisBankStatementParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // tranDate  chqNo  particulars (non-greedy)  debit  credit  balance
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "^(\\d{2}-\\d{2}-\\d{4})\\s+(\\S+)\\s+(.+?)\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s*$");

    @Override
    public String bankName() {
        return "AXIS_BANK";
    }

    @Override
    public boolean matches(String extractedText) {
        // The name alone is not enough: other statements mention this bank in passing (e.g. Paytm's
        // "Paid to Axis Bank Limited" or a linked-account label) and would be claimed by the wrong
        // parser, so an actual row in this bank's layout must be present too (found on a real Paytm file).
        return extractedText.toUpperCase().contains("AXIS BANK")
                && StatementParsingSupport.hasMatchingRow(extractedText, ROW_PATTERN);
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        return StatementParsingSupport.parseSixColumnRows(extractedText, bankName(), ROW_PATTERN, DATE_FORMAT);
    }
}
