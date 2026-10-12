package com.finance.application.user;

/**
 * What a person must give to delete their account: the word DELETE, and proof it is really them - their password, or for an
 * account that only ever signed in with Google, a fresh Google credential for that same Google account.
 */
public record DeleteAccountCommand(String password, String googleCredential, String confirmation) {
}
