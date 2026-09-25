package com.finance.infrastructure.web.transaction;

import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionSource;
import com.finance.domain.transaction.TransactionSplit;
import com.finance.domain.transaction.TransactionStatus;
import com.finance.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        UUID merchantId,
        UUID categoryId,
        LocalDate transactionDate,
        BigDecimal amount,
        String currency,
        String description,
        TransactionType transactionType,
        TransactionSource source,
        TransactionStatus status,
        UUID transferGroupId,
        List<SplitResponse> splits) {

    public static TransactionResponse from(Transaction transaction, List<TransactionSplit> splits) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getMerchantId(),
                transaction.getCategoryId(),
                transaction.getTransactionDate(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getTransactionType(),
                transaction.getSource(),
                transaction.getStatus(),
                transaction.getTransferGroupId(),
                splits.stream().map(SplitResponse::from).toList());
    }
}
