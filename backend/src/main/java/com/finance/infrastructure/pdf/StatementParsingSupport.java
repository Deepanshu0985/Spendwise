package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared low-level helpers for the per-bank parsers - date/amount parsing and
 * the common "date, ref, description, debit, credit, balance" row shape most
 * of these statements share, kept in one place instead of tripled across
 * every StatementParser implementation.
 */
final class StatementParsingSupport {

    private StatementParsingSupport() {
    }

    /** Tries each formatter in order; bank statements vary in date format even within one PDF's header vs. rows. */
    static LocalDate parseDate(String raw, DateTimeFormatter... formatters) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(raw.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // try the next formatter
            }
        }
        return null;
    }

    /** Strips thousands separators; a blank/zero column (the common "0.00" placeholder for the unused debit/credit side) returns ZERO. */
    static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        String cleaned = raw.replace(",", "").trim();
        if (cleaned.isEmpty() || cleaned.equals("-")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cleaned);
    }

    /**
     * Parses every line matching rowPattern as (date, ref, description, debit,
     * credit, balance) - the shape Axis Bank, BOB, Ujjivan and Paytm Wallet all
     * happen to share. A parser with a genuinely different column order (HDFC,
     * SBI) still parses its own lines directly rather than forcing this shape.
     */
    static ParsedStatement parseSixColumnRows(String extractedText, String bankName, Pattern rowPattern, DateTimeFormatter dateFormat) {
        List<ParsedTransactionRow> rows = new ArrayList<>();
        String[] lines = extractedText.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = rowPattern.matcher(lines[i].trim());
            if (!matcher.matches()) {
                continue;
            }
            LocalDate date = parseDate(matcher.group(1), dateFormat);
            if (date == null) {
                continue;
            }
            String description = matcher.group(3).trim();
            BigDecimal debit = parseAmount(matcher.group(4));
            BigDecimal credit = parseAmount(matcher.group(5));
            BigDecimal balance = parseAmount(matcher.group(6));
            boolean isDebit = debit.signum() > 0;
            BigDecimal amount = isDebit ? debit : credit;
            if (amount.signum() <= 0) {
                continue;
            }
            rows.add(new ParsedTransactionRow(
                    date, amount, isDebit ? ParsedTransactionRow.DebitCredit.DEBIT : ParsedTransactionRow.DebitCredit.CREDIT,
                    description, matcher.group(2), balance, "p1r" + (i + 1)));
        }
        LocalDate periodStart = rows.stream().map(ParsedTransactionRow::transactionDate).min(Comparator.naturalOrder()).orElse(null);
        LocalDate periodEnd = rows.stream().map(ParsedTransactionRow::transactionDate).max(Comparator.naturalOrder()).orElse(null);
        return new ParsedStatement(bankName, periodStart, periodEnd, rows);
    }
}
