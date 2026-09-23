package com.finance.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stand-in until Resend (ADR-016) is actually wired up with a real API key
 * (D-04/MAIL_API_KEY is still unset). Deliberately never logs the body: it
 * contains the password-reset link, which carries the reset token, and
 * security.md forbids logging tokens even for debugging - that rule doesn't
 * stop applying just because this is a stub. End-to-end password-reset testing
 * needs the real ResendEmailSender in place.
 */
@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String toAddress, String subject, String body) {
        log.info("Email suppressed (no email provider configured yet, see ADR-016/D-04): to={}, subject={}",
                toAddress, subject);
    }
}
