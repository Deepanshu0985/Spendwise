package com.finance.application.exception;

import org.springframework.http.HttpStatus;

/**
 * The file is a readable PDF of a supported bank, but something about processing
 * it still failed - encrypted, no extractable text (a scanned/image-only PDF;
 * OCR is deliberately deferred, see DECISIONS.md), or malformed content that
 * defeats parsing. The Statement itself moves to FAILED with this message; the
 * user can fix the file and retry (POST /statements/{id}/retry).
 */
public class StatementProcessingFailedException extends ApiException {

    public StatementProcessingFailedException(String message) {
        super(ErrorCode.STATEMENT_PROCESSING_FAILED, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
