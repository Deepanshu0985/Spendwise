package com.finance.transaction;

public record TransferResponse(TransactionResponse outTransaction, TransactionResponse inTransaction) {
}
