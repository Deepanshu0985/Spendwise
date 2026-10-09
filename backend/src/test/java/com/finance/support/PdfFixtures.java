package com.finance.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Synthetic, in-memory-generated PDF fixtures for statement-import tests
 * (testing-strategy.md: "Fixtures are synthetic or hand-redacted; real financial
 * data is never committed."). Generated at test-run time with PDFBox itself
 * rather than committed as binary files - same synthetic-data guarantee, no
 * binary diffs to maintain.
 */
public final class PdfFixtures {

    private PdfFixtures() {
    }

    /** One line of text per row, matching the exact column layout HdfcBankStatementParser/SbiStatementParser/AxisBankStatementParser expect. */
    public static byte[] statementPdf(String headerLine, List<String> rowLines) {
        return buildStatementPdf(headerLine, rowLines, null);
    }

    /** Same as statementPdf, but opening it requires userPassword - a made-up test value, never a real one. */
    public static byte[] encryptedStatementPdf(String headerLine, List<String> rowLines, String userPassword) {
        return buildStatementPdf(headerLine, rowLines, userPassword);
    }

    private static byte[] buildStatementPdf(String headerLine, List<String> rowLines, String userPassword) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                stream.setFont(font, 10);
                stream.beginText();
                stream.newLineAtOffset(40, 780);
                stream.showText(headerLine);
                stream.endText();

                float y = 750;
                for (String line : rowLines) {
                    stream.beginText();
                    stream.newLineAtOffset(40, y);
                    stream.showText(line);
                    stream.endText();
                    y -= 15;
                }
            }
            if (userPassword != null) {
                StandardProtectionPolicy policy = new StandardProtectionPolicy("owner-" + userPassword, userPassword, new AccessPermission());
                policy.setEncryptionKeyLength(128);
                document.protect(policy);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build synthetic PDF fixture", e);
        }
    }

    /** A blank page with no extractable text - simulates a scanned/image-only statement. */
    public static byte[] scannedLookingPdf() {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.A4));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build synthetic PDF fixture", e);
        }
    }

    /** A password-protected PDF - PdfTextExtractor must reject this cleanly. */
    public static byte[] encryptedPdf() {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                stream.setFont(font, 10);
                stream.beginText();
                stream.newLineAtOffset(40, 780);
                stream.showText("HDFC BANK statement");
                stream.endText();
            }
            AccessPermission permission = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner-secret", "user-secret", permission);
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build synthetic PDF fixture", e);
        }
    }

    /** Not a PDF at all - for signature-check rejection. */
    public static byte[] notAPdf() {
        return "this is not a pdf file".getBytes();
    }
}
