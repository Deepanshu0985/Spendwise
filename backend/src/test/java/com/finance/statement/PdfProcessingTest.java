package com.finance.statement;

import com.finance.application.exception.ApiException;
import com.finance.application.exception.FileTooLargeException;
import com.finance.application.exception.StatementProcessingFailedException;
import com.finance.application.exception.UnsupportedFileException;
import com.finance.infrastructure.pdf.ExtractionResult;
import com.finance.infrastructure.pdf.PdfTextExtractor;
import com.finance.infrastructure.pdf.PdfValidator;
import com.finance.support.PdfFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * security.md's validation checklist (MIME/signature/size/page count/encryption),
 * exercised against synthetic PDFs (testing-strategy.md: text, scanned, malformed,
 * encrypted, multi-page fixtures) - no Spring context needed for either class.
 */
class PdfProcessingTest {

    private final PdfValidator validator = new PdfValidator(10_485_760L);
    private final PdfTextExtractor extractor = new PdfTextExtractor(100);

    @Test
    void rejectsAFileThatIsNotAPdfBySignature() {
        assertThatThrownBy(() -> validator.validateUploadShape(PdfFixtures.notAPdf(), "application/pdf"))
                .isInstanceOf(UnsupportedFileException.class);
    }

    @Test
    void rejectsAMismatchedDeclaredContentType() {
        byte[] pdf = PdfFixtures.statementPdf("HDFC BANK", List.of());
        assertThatThrownBy(() -> validator.validateUploadShape(pdf, "image/png"))
                .isInstanceOf(UnsupportedFileException.class);
    }

    @Test
    void rejectsAFileOverTheConfiguredSizeLimit() {
        PdfValidator tinyLimitValidator = new PdfValidator(10L);
        byte[] pdf = PdfFixtures.statementPdf("HDFC BANK", List.of());
        assertThatThrownBy(() -> tinyLimitValidator.validateUploadShape(pdf, "application/pdf"))
                .isInstanceOf(FileTooLargeException.class);
    }

    @Test
    void acceptsAWellFormedPdf() {
        byte[] pdf = PdfFixtures.statementPdf("HDFC BANK", List.of("15/09/26 TEST REF 15/09/26 100.00 0.00 900.00"));
        validator.validateUploadShape(pdf, "application/pdf");
        // no exception
    }

    @Test
    void extractsTextFromAWellFormedPdf() {
        byte[] pdf = PdfFixtures.statementPdf("HDFC BANK Statement", List.of("15/09/26 TEST REF 15/09/26 100.00 0.00 900.00"));
        ExtractionResult result = extractor.extract(pdf);
        assertThat(result.text()).contains("HDFC BANK Statement");
        assertThat(result.pageCount()).isEqualTo(1);
    }

    @Test
    void rejectsAnEncryptedPdf() {
        byte[] pdf = PdfFixtures.encryptedPdf();
        assertThatThrownBy(() -> extractor.extract(pdf))
                .isInstanceOfAny(StatementProcessingFailedException.class, ApiException.class);
    }

    @Test
    void rejectsAScannedLookingPdfWithNoExtractableText() {
        byte[] pdf = PdfFixtures.scannedLookingPdf();
        assertThatThrownBy(() -> extractor.extract(pdf))
                .isInstanceOf(StatementProcessingFailedException.class);
    }
}
