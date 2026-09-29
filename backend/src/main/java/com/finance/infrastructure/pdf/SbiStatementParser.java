package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * State Bank of India's statement layout: Txn Date | Value Date | Description |
 * Ref No./Cheque No. | Debit | Credit | Balance.
 */
@Component
public class SbiStatementParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // txnDate  valueDate  description (non-greedy)  refNo  debit  credit  balance
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "^(\\d{2}-\\d{2}-\\d{4})\\s+(\\d{2}-\\d{2}-\\d{4})\\s+(.+?)\\s+(\\S+)\\s+"
                    + "([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s*$");

    @Override
    public String bankName() {
        return "SBI";
    }

    @Override
    public boolean matches(String extractedText) {
        // "STATE BANK OF INDIA" only, not the bare "SBI" substring - real statements say the full
        // name prominently in the header, and the abbreviation alone risks false-matching other banks'
        // narration text (e.g. "NEFT to SBI a/c" appearing inside an HDFC or Axis statement).
        return extractedText.toUpperCase().contains("STATE BANK OF INDIA");
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        List<ParsedTransactionRow> rows = new ArrayList<>();
        String[] lines = extractedText.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = ROW_PATTERN.matcher(lines[i].trim());
            if (!matcher.matches()) {
                continue;
            }
            LocalDate date = StatementParsingSupport.parseDate(matcher.group(1), DATE_FORMAT);
            if (date == null) {
                continue;
            }
            String description = matcher.group(3).trim();
            var debit = StatementParsingSupport.parseAmount(matcher.group(5));
            var credit = StatementParsingSupport.parseAmount(matcher.group(6));
            var balance = StatementParsingSupport.parseAmount(matcher.group(7));
            boolean isDebit = debit.signum() > 0;
            var amount = isDebit ? debit : credit;
            if (amount.signum() <= 0) {
                continue;
            }
            rows.add(new ParsedTransactionRow(
                    date, amount, isDebit ? ParsedTransactionRow.DebitCredit.DEBIT : ParsedTransactionRow.DebitCredit.CREDIT,
                    description, matcher.group(4), balance, "p1r" + (i + 1)));
        }
        LocalDate periodStart = rows.stream().map(ParsedTransactionRow::transactionDate).min(Comparator.naturalOrder()).orElse(null);
        LocalDate periodEnd = rows.stream().map(ParsedTransactionRow::transactionDate).max(Comparator.naturalOrder()).orElse(null);
        return new ParsedStatement(bankName(), periodStart, periodEnd, rows);
    }
}
