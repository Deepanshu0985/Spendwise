package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;

/**
 * One implementation per supported bank/wallet (ADR-014: HDFC Bank, SBI, Axis
 * Bank, Bank of Baroda, Ujjivan Small Finance Bank, Paytm Wallet - the banks
 * the user actually holds accounts with). Operates on already-extracted text,
 * never on the raw PDF bytes - PdfTextExtractor owns extraction, this owns
 * turning that text into rows.
 */
public interface StatementParser {

    /** The bank this parser handles, used by StatementFormatDetector to pick one. */
    String bankName();

    /** Whether the extracted text looks like this bank's statement format. */
    boolean matches(String extractedText);

    ParsedStatement parse(String extractedText);
}
