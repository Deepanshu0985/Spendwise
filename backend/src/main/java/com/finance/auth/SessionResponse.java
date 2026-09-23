package com.finance.auth;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID id, Instant createdAt, Instant lastSeenAt, String userAgent) {

    public static SessionResponse from(Session session) {
        return new SessionResponse(session.getId(), session.getCreatedAt(), session.getLastSeenAt(), session.getUserAgent());
    }
}
