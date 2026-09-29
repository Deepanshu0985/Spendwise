package com.finance.application.exception;

import org.springframework.http.HttpStatus;

/** An uploaded file exceeds the configured size or page-count limit (security.md's validation checklist). */
public class FileTooLargeException extends ApiException {

    public FileTooLargeException(String message) {
        super(ErrorCode.FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE, message);
    }
}
