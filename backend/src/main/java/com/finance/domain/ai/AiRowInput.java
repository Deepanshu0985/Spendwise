package com.finance.domain.ai;

import java.util.UUID;

/** One row as the model sees it: no id, no amount, no account - just the cleaned text and which way the money moved. */
public record AiRowInput(UUID rowId, String description, boolean debit) {
}
