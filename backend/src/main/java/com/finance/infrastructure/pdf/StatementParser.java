package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;

/**
 * One implementation per supported bank/wallet. Supporting another bank means writing one class that
 * implements this interface (see docs/05-statement-processing/adding-a-bank.md) - Spring discovers it
 * and StatementParserContractTest automatically checks it against every other bank. Operates on
 * already-extracted text, never on the raw PDF bytes - PdfTextExtractor owns extraction, this owns
 * turning that text into rows.
 */
public interface StatementParser {

    /** The bank this parser handles, used by StatementFormatDetector to pick one. */
    String bankName();

    /** Name shown to users (e.g. in the list of supported banks), such as "Bank of Baroda". */
    String displayName();

    /**
     * Whether the extracted text is this bank's statement format. Must not be fooled by another bank's
     * name appearing in narration or a linked-account label: require this bank's own structure (a row in
     * its layout, or its own section title), not just its name.
     */
    boolean matches(String extractedText);

    /**
     * Where in the text this bank identifies itself (the index of its name, title or footer). Only used to
     * break a tie when more than one parser's matches() is true: a statement names its own bank in the
     * letterhead, before any narration that merely mentions another bank. Called only after matches()
     * returned true for the same text.
     */
    int identityPosition(String extractedText);

    ParsedStatement parse(String extractedText);
}
