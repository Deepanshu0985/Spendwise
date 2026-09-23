package com.finance.infrastructure.mail;

/**
 * The only path to an email provider, mirroring AIModelClient's role as the
 * single egress chokepoint for its own external dependency (lld.md). Not
 * listed under infrastructure/{pdf,ocr,ai,storage} in lld.md's original
 * package sketch - added here since Phase 1's password reset flow needs it and
 * the same chokepoint principle applies.
 */
public interface EmailSender {

    void send(String toAddress, String subject, String body);
}
