package com.finance.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Reused across POST /transactions, POST /transactions/transfer and
 * eventually POST /statements/{id}/confirm (api-specification.md's
 * Idempotency section) - a repeated key returns exactly the first call's
 * response instead of re-running the action.
 */
public interface IdempotencyService {

    ResponseEntity<?> executeIdempotent(
            UUID userId, String idempotencyKey, String endpoint, HttpStatus successStatus, Supplier<Object> action);
}
