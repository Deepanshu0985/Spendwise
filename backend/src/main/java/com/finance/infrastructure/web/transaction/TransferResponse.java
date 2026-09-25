package com.finance.infrastructure.web.transaction;

public record TransferResponse(TransactionResponse outTransaction, TransactionResponse inTransaction) {
}
