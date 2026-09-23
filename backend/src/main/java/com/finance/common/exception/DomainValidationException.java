package com.finance.common.exception;

import com.finance.common.error.ApiError;
import com.finance.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * For business-rule violations bean validation cannot express, e.g. splits
 * that do not sum to the parent transaction amount (ADR-013).
 */
public class DomainValidationException extends ApiException {

    private final List<ApiError.Detail> details;

    public DomainValidationException(String message, List<ApiError.Detail> details) {
        super(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
        this.details = details;
    }

    public List<ApiError.Detail> details() {
        return details;
    }
}
