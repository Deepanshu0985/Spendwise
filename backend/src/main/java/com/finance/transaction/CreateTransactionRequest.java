package com.finance.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull UUID accountId,
        UUID merchantId,
        UUID categoryId,
        @NotNull LocalDate transactionDate,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Size(min = 3, max = 3) String currency,
        @Size(max = 500) String description,
        @NotNull TransactionType transactionType,
        @Valid List<SplitRequest> splits) {
}
