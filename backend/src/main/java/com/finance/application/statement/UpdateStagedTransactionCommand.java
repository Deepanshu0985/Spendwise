package com.finance.application.statement;

import com.finance.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateStagedTransactionCommand(
        LocalDate transactionDate, BigDecimal amount, UUID merchantId, UUID categoryId, TransactionType transactionType) {
}
