package com.finance.transaction;

import java.time.LocalDate;
import java.util.UUID;

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
