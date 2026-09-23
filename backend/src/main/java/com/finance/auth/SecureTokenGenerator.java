package com.finance.auth;

/** High-entropy random tokens for session cookies and password reset links. */
public interface SecureTokenGenerator {

    String generate();
}
