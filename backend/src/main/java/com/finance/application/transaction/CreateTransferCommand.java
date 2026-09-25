package com.finance.application.transaction;

import com.finance.domain.transaction.TransferKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransferCommand(
        UUID fromAccountId,
        UUID toAccountId,
        LocalDate transactionDate,
        BigDecimal amount,
        String currency,
        TransferKind kind) {
}
