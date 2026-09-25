package com.finance.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransferRequest(
        @NotNull UUID fromAccountId,
        @NotNull UUID toAccountId,
        @NotNull LocalDate transactionDate,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Size(min = 3, max = 3) String currency,
        @NotNull TransferKind kind) {
}
