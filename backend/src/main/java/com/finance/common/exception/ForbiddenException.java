package com.finance.common.exception;

import com.finance.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** The request is authenticated but rejected for a reason other than resource ownership (which returns 404). */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, message);
    }
}
