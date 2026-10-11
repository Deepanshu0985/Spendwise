package com.finance.infrastructure.web.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The word DELETE, plus the person's password, or (for a Google-only account) a fresh Google credential. */
public record DeleteAccountRequest(
        @Size(max = 200) String password, @Size(max = 8192) String googleCredential, @NotBlank @Size(max = 20) String confirmation) {
}
