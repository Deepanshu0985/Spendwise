package com.finance.application.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown for any resource the caller does not own or that does not exist.
 * Per 03-api/error-contract.md, ownership failures return 404, never 403 -
 * returning 403 would confirm the identifier exists across tenants.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
