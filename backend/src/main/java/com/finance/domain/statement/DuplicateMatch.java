package com.finance.domain.statement;

import java.util.UUID;

/** A scored match between a staged row and an already-imported transaction. */
public record DuplicateMatch(DuplicateStatus status, DuplicateReason reason, UUID matchedTransactionId) {
}
