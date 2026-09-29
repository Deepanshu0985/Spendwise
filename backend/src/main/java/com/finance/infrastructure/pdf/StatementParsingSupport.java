package com.finance.infrastructure.pdf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Shared low-level helpers for the per-bank parsers - date/amount parsing that
 * every statement format needs, kept in one place instead of tripled across
 * HdfcBankStatementParser/SbiStatementParser/AxisBankStatementParser.
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
}
