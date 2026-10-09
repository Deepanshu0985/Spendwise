package com.finance.infrastructure.pdf;

import com.finance.application.exception.FileTooLargeException;
import com.finance.application.exception.StatementProcessingFailedException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
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
        return extract(content, null);
    }

    /** password (nullable) opens the PDF in memory only; it is never stored or logged, and never put in an error message. */
    public ExtractionResult extract(byte[] content, String password) {
        try (PDDocument document = Loader.loadPDF(content, password == null ? "" : password)) {
            // Opening succeeded, so any password needed was supplied. An encrypted file is fine on its own (many
            // banks only restrict printing/copying); what matters is whether its text may be read at all.
            if (document.isEncrypted() && !document.getCurrentAccessPermission().canExtractContent()) {
                throw new StatementProcessingFailedException("This PDF doesn't allow its text to be read, so it can't be imported.");
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
        } catch (InvalidPasswordException e) {
            throw new StatementProcessingFailedException(password == null || password.isEmpty()
                    ? "This PDF is password-protected. Enter its password to continue."
                    : "That password is incorrect for this PDF.");
        } catch (IOException e) {
            throw new StatementProcessingFailedException("Could not read this PDF file: " + e.getMessage());
        }
    }
}
