package com.finance.domain.transaction;

import java.time.LocalDate;
import java.util.UUID;

/** Query criteria for TransactionRepository.search - a plain value object, so it can be a repository-port parameter type. */
public record TransactionFilter(
        LocalDate from,
        LocalDate to,
        UUID accountId,
        UUID categoryId,
        UUID merchantId,
        TransactionType type,
        TransactionStatus status,
        String currency) {
}
