package com.finance.application.exception;

import org.springframework.http.HttpStatus;

/** A per-user or global AI cap is used up. The message says which one and when it resets (error-contract.md). */
public class AiQuotaExceededException extends ApiException {

    public AiQuotaExceededException(String message) {
        super(ErrorCode.AI_QUOTA_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
