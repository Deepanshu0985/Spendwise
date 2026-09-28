package com.finance.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Pure cookie-header construction, not swappable business behavior - no
 * interface needed here (coding-standards.md's SOLID rule targets services with
 * real alternate implementations, not deterministic formatting utilities).
 */
@Component
public class SessionCookieFactory {

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;

    public SessionCookieFactory(
            @Value("${session.cookie-name}") String cookieName,
            @Value("${security.cookie-secure:true}") boolean secure,
            @Value("${security.cookie-samesite:Lax}") String sameSite) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    /** httpOnly, Secure (except in plain-http local dev), SameSite per config, Path=/ per ADR-009 and authentication-api.md. */
    public ResponseCookie create(String rawToken, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        return ResponseCookie.from(cookieName, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String cookieName() {
        return cookieName;
    }
}
