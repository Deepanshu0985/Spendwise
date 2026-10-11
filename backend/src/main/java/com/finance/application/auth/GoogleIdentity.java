package com.finance.application.auth;

/** What a verified Google ID token says about the person: Google's stable account id, their email and whether Google has verified it. */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
}
