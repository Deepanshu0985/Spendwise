package com.finance.application.auth;

import com.finance.domain.user.User;
import com.finance.infrastructure.security.Session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuthService {

    User register(String email, String rawPassword, String fullName);

    /** @param clientIp the caller's address, used only to count failed guesses; never stored or logged */
    LoginResult login(String email, String rawPassword, String userAgent, String clientIp);

    /** Signs in (or signs up) with a Google ID token. Throws UnauthorizedException for an invalid token. */
    LoginResult loginWithGoogle(String idToken, String userAgent);

    /** The Google client id when Google sign-in has been set up on this server, otherwise empty. */
    java.util.Optional<String> googleClientId();

    void logout(UUID sessionId);

    List<Session> listActiveSessions(UUID userId);

    void revokeAllSessions(UUID userId);

    void requestPasswordReset(String email);

    void confirmPasswordReset(String rawToken, String newPassword);

    record LoginResult(User user, String rawSessionToken, Instant expiresAt) {
    }
}
