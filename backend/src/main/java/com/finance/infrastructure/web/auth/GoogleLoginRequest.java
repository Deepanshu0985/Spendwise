package com.finance.infrastructure.web.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The "credential" Google's sign-in button returns: a signed ID token. */
public record GoogleLoginRequest(@NotBlank @Size(max = 8192) String credential) {
}
