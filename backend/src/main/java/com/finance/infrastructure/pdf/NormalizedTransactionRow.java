package com.finance.infrastructure.pdf;

import com.finance.domain.transaction.TransactionType;

import java.math.BigDecimal;

/** TransactionNormalizer's output for one parsed row: normalized text plus a classified type and confidence. */
public record NormalizedTransactionRow(String normalizedDescription, TransactionType transactionType, BigDecimal confidenceScore) {
}
