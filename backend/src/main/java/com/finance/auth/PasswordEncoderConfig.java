package com.finance.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder is Spring Security's own interface - consuming code depends on
 * that, never on BCryptPasswordEncoder directly, satisfying program-to-an-
 * interface without a redundant custom wrapper around an interface that already
 * exists (coding-standards.md).
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
