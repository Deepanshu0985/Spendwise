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
 * HDFC Bank's statement layout: Date | Narration | Chq/Ref No | Value Date |
 * Withdrawal Amt | Deposit Amt | Closing Balance. One row per line once
 * extracted; Chq/Ref No and Value Date are read but not currently persisted
 * (source_row_reference keeps the original line for audit instead).
 */
@Component
public class HdfcBankStatementParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy");

    // date  narration (non-greedy)  refNo  valueDate  withdrawal  deposit  balance
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{2})\\s+(.+?)\\s+(\\S+)\\s+(\\d{2}/\\d{2}/\\d{2})\\s+"
                    + "([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})\\s*$");

    @Override
    public String bankName() {
        return "HDFC_BANK";
    }

    @Override
    public boolean matches(String extractedText) {
        return extractedText.toUpperCase().contains("HDFC BANK");
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
            String narration = matcher.group(2).trim();
            var withdrawal = StatementParsingSupport.parseAmount(matcher.group(5));
            var deposit = StatementParsingSupport.parseAmount(matcher.group(6));
            var balance = StatementParsingSupport.parseAmount(matcher.group(7));
            boolean isDebit = withdrawal.signum() > 0;
            var amount = isDebit ? withdrawal : deposit;
            if (amount.signum() <= 0) {
                continue;
            }
            rows.add(new ParsedTransactionRow(
                    date, amount, isDebit ? ParsedTransactionRow.DebitCredit.DEBIT : ParsedTransactionRow.DebitCredit.CREDIT,
                    narration, matcher.group(3), balance, "p1r" + (i + 1)));
        }
        LocalDate periodStart = rows.stream().map(ParsedTransactionRow::transactionDate).min(Comparator.naturalOrder()).orElse(null);
        LocalDate periodEnd = rows.stream().map(ParsedTransactionRow::transactionDate).max(Comparator.naturalOrder()).orElse(null);
        return new ParsedStatement(bankName(), periodStart, periodEnd, rows);
    }
}
