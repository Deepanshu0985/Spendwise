package com.finance.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Used for both create and update - the two are identical in shape (just a name), so no separate DTOs. */
public record MerchantNameRequest(@NotBlank @Size(max = 255) String canonicalName) {
}
