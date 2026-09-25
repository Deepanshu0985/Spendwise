package com.finance.application.exception;

/** Codes from 03-api/error-contract.md. Keep this enum in sync with that document. */
public enum ErrorCode {
    VALIDATION_ERROR,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    FILE_TOO_LARGE,
    UNSUPPORTED_FILE,
    STATEMENT_PROCESSING_FAILED,
    RATE_LIMITED,
    AI_QUOTA_EXCEEDED,
    AI_UNAVAILABLE,
    INTERNAL_ERROR
}
