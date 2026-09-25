package com.finance.infrastructure.web.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** accountType and currency are immutable after creation - changing either would
 * put existing transaction history and analytics in an inconsistent state. */
public record UpdateAccountRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String institutionName,
        @Size(max = 4) String last4) {
}
