package com.finance.infrastructure.web.statement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** label is the source account text exactly as the statement row shows it (StatementTransactionResponse.sourceAccountLabel). */
public record MapSourceAccountRequest(@NotBlank String label, @NotNull UUID accountId) {
}
