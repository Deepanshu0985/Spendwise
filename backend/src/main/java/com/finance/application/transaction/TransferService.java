package com.finance.application.transaction;

import com.finance.domain.transaction.Transaction;

import java.util.UUID;

public interface TransferService {

    TransferResult create(UUID userId, CreateTransferCommand command);

    record TransferResult(Transaction outTransaction, Transaction inTransaction) {
    }
}
