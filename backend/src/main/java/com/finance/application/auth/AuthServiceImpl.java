package com.finance.application.auth;

import com.finance.application.exception.ApiError;
import com.finance.application.exception.ConflictException;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.UnauthorizedException;
import com.finance.domain.user.User;
import com.finance.domain.user.UserRepository;
import com.finance.infrastructure.mail.EmailSender;
import com.finance.infrastructure.security.CreatedSession;
import com.finance.infrastructure.security.PasswordResetToken;
import com.finance.infrastructure.security.PasswordResetTokenRepository;
import com.finance.infrastructure.security.SecureTokenGenerator;
import com.finance.infrastructure.security.Session;
import com.finance.infrastructure.security.SessionRepository;
import com.finance.infrastructure.security.SessionStore;
import com.finance.infrastructure.security.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String GENERIC_LOGIN_FAILURE = "Invalid email or password.";

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SessionStore sessionStore;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final Duration passwordResetTtl;
    private final String appBaseUrl;

    public AuthServiceImpl(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            SessionStore sessionStore,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            @Value("${password-reset.ttl}") Duration passwordResetTtl,
            @Value("${APP_BASE_URL:http://localhost:5173}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.sessionStore = sessionStore;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.passwordResetTtl = passwordResetTtl;
        this.appBaseUrl = appBaseUrl;
    }

    @Override
    @Transactional
    public User register(String email, String rawPassword, String fullName) {
        String normalizedEmail = email.strip().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists.");
        }
        User user = new User(normalizedEmail, passwordEncoder.encode(rawPassword), fullName, "INR", "Asia/Kolkata");
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public LoginResult login(String email, String rawPassword, String userAgent) {
        User user = userRepository.findByEmail(email.strip().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException(GENERIC_LOGIN_FAILURE));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException(GENERIC_LOGIN_FAILURE);
        }

        CreatedSession created = sessionStore.create(user.getId(), userAgent);
        return new LoginResult(user, created.rawToken(), created.session().getExpiresAt());
    }

    @Override
    @Transactional
    public void logout(UUID sessionId) {
        if (sessionId != null) {
            sessionStore.revoke(sessionId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> listActiveSessions(UUID userId) {
        Instant now = Instant.now();
        return sessionRepository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .filter(session -> session.isActive(now))
                .toList();
    }

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId) {
        Instant now = Instant.now();
        List<Session> sessions = sessionRepository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId);
        sessions.forEach(session -> session.revoke(now));
        sessionRepository.saveAll(sessions);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        // Always returns normally regardless of whether the email exists - the
        // controller returns 202 either way, per authentication-api.md, so this
        // never discloses which emails are registered.
        userRepository.findByEmail(email.strip().toLowerCase()).ifPresent(user -> {
            String rawToken = tokenGenerator.generate();
            String tokenHash = tokenHasher.hash(rawToken);
            Instant expiresAt = Instant.now().plus(passwordResetTtl);
            passwordResetTokenRepository.save(new PasswordResetToken(user.getId(), tokenHash, expiresAt));

            String resetLink = appBaseUrl + "/reset-password?token=" + rawToken;
            emailSender.send(
                    user.getEmail(),
                    "Reset your password",
                    "Use the link below to reset your password. It expires in "
                            + passwordResetTtl.toHours() + " hour(s).\n\n" + resetLink);
        });
    }

    @Override
    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        String tokenHash = tokenHasher.hash(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .filter(token -> token.isUsable(Instant.now()))
                .orElseThrow(() -> new DomainValidationException(
                        "One or more fields are invalid.",
                        List.of(new ApiError.Detail("token", "invalid or expired"))));

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new DomainValidationException(
                        "One or more fields are invalid.",
                        List.of(new ApiError.Detail("token", "invalid or expired"))));

        user.changePasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.markUsed(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        revokeAllSessions(user.getId());
    }
}
