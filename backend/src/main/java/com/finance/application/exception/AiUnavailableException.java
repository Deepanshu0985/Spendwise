package com.finance.application.exception;

import org.springframework.http.HttpStatus;

/** The model could not be reached or gave nothing usable. Everything else in the product keeps working (ai-architecture.md: Failure). */
public class AiUnavailableException extends ApiException {

    public AiUnavailableException(String message) {
        super(ErrorCode.AI_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
