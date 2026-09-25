package com.finance.application.transaction;

import com.finance.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTransactionCommand(
        UUID accountId,
        UUID merchantId,
        UUID categoryId,
        LocalDate transactionDate,
        BigDecimal amount,
        String currency,
        String description,
        TransactionType transactionType,
        List<SplitCommand> splits) {
}
