package com.finance.transaction;

import java.math.BigDecimal;
import java.util.UUID;

public record SplitResponse(UUID categoryId, BigDecimal amount) {

    public static SplitResponse from(TransactionSplit split) {
        return new SplitResponse(split.getCategoryId(), split.getAmount());
    }
}
