package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Bank of Baroda's statement layout: Date | Cheque/Ref No | Narration | Debit
 * | Credit | Balance - the same six-column shape as Axis Bank, Ujjivan and
 * Paytm Wallet (see StatementParsingSupport.parseSixColumnRows).
 */
@Component
public class BobStatementParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // date  refNo  narration (non-greedy)  debit  credit  balance
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "^(\\d{2}-\\d{2}-\\d{4})\\s+(\\S+)\\s+(.+?)\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s*$");

    @Override
    public String bankName() {
        return "BOB";
    }

    @Override
    public boolean matches(String extractedText) {
        String upper = extractedText.toUpperCase();
        // Full name only, not a bare "BOB" substring - avoids false-matching narration
        // text like "NEFT TO BOB A/C" inside another bank's statement (same reasoning
        // as SbiStatementParser avoiding the bare "SBI" substring).
        return upper.contains("BANK OF BARODA");
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        return StatementParsingSupport.parseSixColumnRows(extractedText, bankName(), ROW_PATTERN, DATE_FORMAT);
    }
}
