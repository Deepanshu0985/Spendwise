package com.finance.infrastructure.pdf;

import com.finance.application.exception.FileTooLargeException;
import com.finance.application.exception.StatementProcessingFailedException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Native text extraction only (pdf-processing.md). No OCR fallback in this
 * phase (see DECISIONS.md) - a PDF with no meaningfully extractable text (a
 * scanned/image-only statement) fails clearly here instead of being silently
 * mis-parsed, rather than half-building an OCR path this project doesn't need
 * yet.
 */
@Component
public class PdfTextExtractor {

    // Real statement pages have hundreds of characters of narration/dates/amounts;
    // a near-empty extraction is the signal a page is a scanned image, not text.
    private static final int MIN_MEANINGFUL_TEXT_LENGTH = 40;

    private final int maxPages;

    public PdfTextExtractor(@Value("${statement.upload.max-pages:100}") int maxPages) {
        this.maxPages = maxPages;
    }

    public ExtractionResult extract(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw new StatementProcessingFailedException(
                        "This PDF is password-protected. Remove the password and upload again.");
            }
            int pageCount = document.getNumberOfPages();
            if (pageCount > maxPages) {
                throw new FileTooLargeException("The statement has " + pageCount + " pages; the limit is " + maxPages + ".");
            }
            String text = new PDFTextStripper().getText(document);
            if (text == null || text.trim().length() < MIN_MEANINGFUL_TEXT_LENGTH) {
                throw new StatementProcessingFailedException(
                        "No readable text found in this PDF - it looks like a scanned document, which isn't supported yet.");
            }
            return new ExtractionResult(text, pageCount);
        } catch (IOException e) {
            throw new StatementProcessingFailedException("Could not read this PDF file: " + e.getMessage());
        }
    }
}
