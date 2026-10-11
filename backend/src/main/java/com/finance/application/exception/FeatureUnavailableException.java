package com.finance.application.exception;

import org.springframework.http.HttpStatus;

/** An optional feature (such as Sign in with Google) has not been set up on this server. */
public class FeatureUnavailableException extends ApiException {

    public FeatureUnavailableException(String message) {
        super(ErrorCode.FEATURE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
