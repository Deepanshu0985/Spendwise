package com.finance.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Purges expired and revoked session rows, per authentication-api.md's session lifecycle. */
@Component
public class SessionCleanupJob {

    private final SessionRepository sessionRepository;

    public SessionCleanupJob(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void purgeExpiredAndRevoked() {
        sessionRepository.deleteByExpiresAtBeforeOrRevokedAtIsNotNull(Instant.now());
    }
}
