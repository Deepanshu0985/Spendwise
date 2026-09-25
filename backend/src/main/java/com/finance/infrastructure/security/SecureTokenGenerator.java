package com.finance.infrastructure.security;

/** High-entropy random tokens for session cookies and password reset links. */
public interface SecureTokenGenerator {

    String generate();
}
