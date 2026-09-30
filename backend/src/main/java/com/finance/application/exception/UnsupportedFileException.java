package com.finance.application.exception;

import org.springframework.http.HttpStatus;

/**
 * The uploaded file isn't a PDF at all (bad signature/MIME), or its bank/format
 * isn't one ADR-014 ships (see the registered StatementParser implementations,
 * infrastructure.pdf) - an unrecognized format fails explicitly rather than
 * attempting a best-effort parse (pdf-processing.md).
 */
public class UnsupportedFileException extends ApiException {

    public UnsupportedFileException(String message) {
        super(ErrorCode.UNSUPPORTED_FILE, HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    }
}
