package com.finance.application.transaction;

import com.finance.domain.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** transactionType, source and currency are immutable after creation - same reasoning as accountType/currency on accounts. */
public record UpdateTransactionCommand(
        UUID accountId,
        UUID merchantId,
        UUID categoryId,
        LocalDate transactionDate,
        BigDecimal amount,
        String description,
        TransactionStatus status,
        List<SplitCommand> splits) {
}
