package com.finance.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class SessionStoreImpl implements SessionStore {

    private final SessionRepository sessionRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final Duration sessionTtl;
    private final Duration idleTimeout;

    public SessionStoreImpl(
            SessionRepository sessionRepository,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            @Value("${session.ttl}") Duration sessionTtl,
            @Value("${session.idle-timeout}") Duration idleTimeout) {
        this.sessionRepository = sessionRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.sessionTtl = sessionTtl;
        this.idleTimeout = idleTimeout;
    }

    @Override
    @Transactional
    public CreatedSession create(UUID userId, String userAgent) {
        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHasher.hash(rawToken);
        Instant expiresAt = Instant.now().plus(sessionTtl);
        Session session = new Session(userId, tokenHash, expiresAt, userAgent);
        sessionRepository.save(session);
        return new CreatedSession(session, rawToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> resolve(String rawToken) {
        String tokenHash = tokenHasher.hash(rawToken);
        Instant now = Instant.now();
        return sessionRepository.findByTokenHash(tokenHash)
                .filter(session -> session.isActive(now))
                .filter(session -> session.getLastSeenAt().plus(idleTimeout).isAfter(now));
    }

    @Override
    @Transactional
    public void revoke(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.revoke(Instant.now());
            sessionRepository.save(session);
        });
    }

    @Override
    @Transactional
    public void touch(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.touch(Instant.now());
            sessionRepository.save(session);
        });
    }
}
