package com.finance.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** transactionType, source and currency are immutable after creation - same reasoning as accountType/currency on accounts. */
public record UpdateTransactionRequest(
        @NotNull UUID accountId,
        UUID merchantId,
        UUID categoryId,
        @NotNull LocalDate transactionDate,
        @NotNull @Positive BigDecimal amount,
        @Size(max = 500) String description,
        TransactionStatus status,
        @Valid List<SplitRequest> splits) {
}
