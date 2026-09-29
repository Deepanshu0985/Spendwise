package com.finance.infrastructure.pdf;

import com.finance.application.exception.FileTooLargeException;
import com.finance.application.exception.UnsupportedFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Pre-open checks only (security.md: "validate MIME type, file signature, size") -
 * cheap enough to run before ever handing the bytes to PDFBox. Page-count and
 * encryption checks need the file actually open, so they live in
 * PdfTextExtractor instead, right next to the code that already opens it.
 */
@Component
public class PdfValidator {

    // "%PDF-" - every well-formed PDF starts with this regardless of version.
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final long maxFileSizeBytes;

    public PdfValidator(@Value("${statement.upload.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public void validateUploadShape(byte[] content, String declaredContentType) {
        if (content.length == 0) {
            throw new UnsupportedFileException("The uploaded file is empty.");
        }
        if (content.length > maxFileSizeBytes) {
            throw new FileTooLargeException(
                    "The uploaded file exceeds the maximum size of " + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }
        // The client-reported MIME type is untrusted input on its own (security.md)
        // - checked alongside the signature, never in place of it.
        if (declaredContentType != null && !declaredContentType.isBlank() && !declaredContentType.equalsIgnoreCase("application/pdf")) {
            throw new UnsupportedFileException("Only PDF statements are supported.");
        }
        if (content.length < PDF_SIGNATURE.length || !Arrays.equals(Arrays.copyOf(content, PDF_SIGNATURE.length), PDF_SIGNATURE)) {
            throw new UnsupportedFileException("The uploaded file is not a valid PDF.");
        }
    }
}
