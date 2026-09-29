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
        return extractedText.toUpperCase().contains("AXIS BANK");
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
            String particulars = matcher.group(3).trim();
            var debit = StatementParsingSupport.parseAmount(matcher.group(4));
            var credit = StatementParsingSupport.parseAmount(matcher.group(5));
            var balance = StatementParsingSupport.parseAmount(matcher.group(6));
            boolean isDebit = debit.signum() > 0;
            var amount = isDebit ? debit : credit;
            if (amount.signum() <= 0) {
                continue;
            }
            rows.add(new ParsedTransactionRow(
                    date, amount, isDebit ? ParsedTransactionRow.DebitCredit.DEBIT : ParsedTransactionRow.DebitCredit.CREDIT,
                    particulars, matcher.group(2), balance, "p1r" + (i + 1)));
        }
        LocalDate periodStart = rows.stream().map(ParsedTransactionRow::transactionDate).min(Comparator.naturalOrder()).orElse(null);
        LocalDate periodEnd = rows.stream().map(ParsedTransactionRow::transactionDate).max(Comparator.naturalOrder()).orElse(null);
        return new ParsedStatement(bankName(), periodStart, periodEnd, rows);
    }
}
