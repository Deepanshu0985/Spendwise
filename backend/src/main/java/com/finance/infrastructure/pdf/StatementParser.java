package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;

/**
 * One implementation per supported bank (ADR-014: HDFC Bank, SBI, Axis Bank).
 * Operates on already-extracted text, never on the raw PDF bytes - PdfTextExtractor
 * owns extraction, this owns turning that text into rows.
 */
public interface StatementParser {

    /** The bank this parser handles, used by StatementFormatDetector to pick one. */
    String bankName();

    /** Whether the extracted text looks like this bank's statement format. */
    boolean matches(String extractedText);

    ParsedStatement parse(String extractedText);
}
