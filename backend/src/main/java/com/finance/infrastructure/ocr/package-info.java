/**
 * Deliberately empty as of Phase 6 (see DECISIONS.md). Native text extraction
 * (infrastructure.pdf.PdfTextExtractor) covers the realistic case - real
 * bank-issued statements are text-based PDFs, not scans - and the user didn't
 * want a local Tesseract install. A scanned/image-only upload fails clearly
 * into FAILED instead. If OCR is ever actually needed, the natural fix is a
 * cloud OCR API call here, not a local native binary.
 */
package com.finance.infrastructure.ocr;
