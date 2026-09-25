package com.finance.infrastructure.web.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record SplitRequest(UUID categoryId, @NotNull @Positive BigDecimal amount) {
}
