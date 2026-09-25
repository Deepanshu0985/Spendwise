package com.finance.domain.transaction;

import java.util.List;

public record TransactionWithSplits(Transaction transaction, List<TransactionSplit> splits) {
}
