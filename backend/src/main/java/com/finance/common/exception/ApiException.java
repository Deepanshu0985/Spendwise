package com.finance.common.exception;

import com.finance.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** Base for domain exceptions the GlobalExceptionHandler maps to the API error contract. */
public abstract class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    protected ApiException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ErrorCode code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
