package com.finance.domain.statement;

import java.time.LocalDate;
import java.util.List;

/** The output of a StatementParser - a detected bank's rows plus the statement's own period bounds. */
public record ParsedStatement(String detectedBank, LocalDate periodStart, LocalDate periodEnd, List<ParsedTransactionRow> rows) {
}
