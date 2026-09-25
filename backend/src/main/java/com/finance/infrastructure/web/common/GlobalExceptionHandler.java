package com.finance.infrastructure.web.common;

import com.finance.application.exception.ApiError;
import com.finance.application.exception.ApiException;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ApiError.Envelope> handleDomainValidation(DomainValidationException ex) {
        ApiError error = new ApiError(ex.code().name(), ex.getMessage(), ex.details(), newRequestId());
        return ResponseEntity.status(ex.status()).body(new ApiError.Envelope(error));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError.Envelope> handleApiException(ApiException ex) {
        ApiError error = new ApiError(ex.code().name(), ex.getMessage(), List.of(), newRequestId());
        return ResponseEntity.status(ex.status()).body(new ApiError.Envelope(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError.Envelope> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<ApiError.Detail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.Detail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ApiError error = new ApiError(
                ErrorCode.VALIDATION_ERROR.name(),
                "One or more fields are invalid.",
                details,
                newRequestId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError.Envelope(error));
    }

    // Never leak the exception message, a stack trace or SQL to the client (07-quality/security.md).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError.Envelope> handleUnexpected(Exception ex) {
        String requestId = newRequestId();
        log.error("Unhandled exception, requestId={}", requestId, ex);
        ApiError error = new ApiError(
                ErrorCode.INTERNAL_ERROR.name(),
                "An unexpected error occurred.",
                List.of(),
                requestId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError.Envelope(error));
    }

    private static String newRequestId() {
        return UUID.randomUUID().toString();
    }
}
