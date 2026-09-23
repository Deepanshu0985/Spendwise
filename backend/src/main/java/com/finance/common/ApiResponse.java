package com.finance.common;

/**
 * Standard response envelope for every endpoint, per 03-api/api-specification.md:
 * {@code {"data": ..., "meta": ...}}. {@code meta} carries pagination and,
 * for analytics, excludedCurrencies/excludedTransactionCount.
 */
public record ApiResponse<T>(T data, Object meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> of(T data, Object meta) {
        return new ApiResponse<>(data, meta);
    }
}
