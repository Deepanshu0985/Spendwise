package com.finance.application.merchant;

/** Used for both create and rename - the two are identical in shape (just a name), so no separate commands. */
public record MerchantNameCommand(String canonicalName) {
}
