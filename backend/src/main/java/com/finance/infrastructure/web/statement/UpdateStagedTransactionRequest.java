package com.finance.infrastructure.web.statement;

import com.finance.domain.transaction.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateStagedTransactionRequest(
        @NotNull LocalDate transactionDate,
        @NotNull @Positive BigDecimal amount,
        UUID merchantId,
        UUID categoryId,
        @NotNull TransactionType transactionType) {
}
