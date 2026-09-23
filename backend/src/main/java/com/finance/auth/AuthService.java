package com.finance.auth;

import com.finance.user.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuthService {

    User register(String email, String rawPassword, String fullName);

    LoginResult login(String email, String rawPassword, String userAgent);

    void logout(UUID sessionId);

    List<Session> listActiveSessions(UUID userId);

    void revokeAllSessions(UUID userId);

    void requestPasswordReset(String email);

    void confirmPasswordReset(String rawToken, String newPassword);

    record LoginResult(User user, String rawSessionToken, Instant expiresAt) {
    }
}
