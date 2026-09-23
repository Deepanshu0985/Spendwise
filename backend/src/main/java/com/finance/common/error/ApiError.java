package com.finance.common.error;

import java.util.List;

/** Body shape from 03-api/error-contract.md: {@code {"error": {code, message, details, requestId}}}. */
public record ApiError(String code, String message, List<Detail> details, String requestId) {

    public record Detail(String field, String reason) {}

    public record Envelope(ApiError error) {}
}
