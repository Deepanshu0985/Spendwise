package com.finance.transaction;

import java.util.UUID;

public interface TransferService {

    TransferResult create(UUID userId, CreateTransferRequest request);

    record TransferResult(Transaction outTransaction, Transaction inTransaction) {
    }
}
