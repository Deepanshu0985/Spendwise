package com.finance.infrastructure.web.account;

import com.finance.domain.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull AccountType accountType,
        @Size(max = 255) String institutionName,
        @Size(max = 4) String last4,
        @NotBlank @Size(min = 3, max = 3) String currency) {
}
