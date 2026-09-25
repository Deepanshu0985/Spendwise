package com.finance.application.transaction;

import java.math.BigDecimal;
import java.util.UUID;

public record SplitCommand(UUID categoryId, BigDecimal amount) {
}
