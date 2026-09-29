package com.finance.infrastructure.pdf;

/** The raw text PdfTextExtractor pulled out of a statement PDF, plus its page count. */
public record ExtractionResult(String text, int pageCount) {
}
