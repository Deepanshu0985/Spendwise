package com.finance.common.exception;

import com.finance.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** Missing, invalid or expired credentials/session. Never states which part was wrong. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }
}
