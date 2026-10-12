package com.finance.application.auth;

import com.finance.application.exception.ApiError;
import com.finance.application.exception.ConflictException;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.RateLimitedException;
import com.finance.application.exception.UnauthorizedException;
import com.finance.application.ratelimit.RateDecision;
import com.finance.application.ratelimit.RateLimiter;
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

    // Failed password guesses: a few per address and account in a quarter of an hour, and a wider ceiling per account across
    // all addresses so a spread-out guesser is still slowed. The per-address key means a stranger cannot lock the real owner
    // out from the owner's own address.
    private static final int MAX_FAILURES_PER_ADDRESS_AND_ACCOUNT = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_FAILURES_PER_ACCOUNT = 25;
    private static final Duration ACCOUNT_FAILURE_WINDOW = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SessionStore sessionStore;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final GoogleIdentityVerifier googleVerifier;
    private final RateLimiter rateLimiter;
    /** Checked against when the account does not exist (or has no password), so every login costs the same time. */
    private final String decoyPasswordHash;
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
            GoogleIdentityVerifier googleVerifier,
            RateLimiter rateLimiter,
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
        this.googleVerifier = googleVerifier;
        this.rateLimiter = rateLimiter;
        this.decoyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
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
    public LoginResult login(String email, String rawPassword, String userAgent, String clientIp) {
        String normalizedEmail = email.strip().toLowerCase();
        String addressKey = "login-fail:" + clientIp + "|" + normalizedEmail;
        String accountKey = "login-fail-account:" + normalizedEmail;

        // Refused the same way whether or not the account exists, before any password is checked.
        RateDecision addressDecision = rateLimiter.check(addressKey, MAX_FAILURES_PER_ADDRESS_AND_ACCOUNT);
        RateDecision accountDecision = rateLimiter.check(accountKey, MAX_FAILURES_PER_ACCOUNT);
        if (!addressDecision.allowed() || !accountDecision.allowed()) {
            throw new RateLimitedException(Math.max(addressDecision.retryAfterSeconds(), accountDecision.retryAfterSeconds()));
        }

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        boolean usable = user != null && user.hasPassword(); // a Google-only account has no password to match
        // Always run one password check, against a decoy when there is no real hash, so an unknown address does not answer
        // faster than a known one (which would reveal which emails are registered).
        boolean matches = passwordEncoder.matches(rawPassword, usable ? user.getPasswordHash() : decoyPasswordHash);
        if (!usable || !matches) {
            rateLimiter.hit(addressKey, FAILURE_WINDOW);
            rateLimiter.hit(accountKey, ACCOUNT_FAILURE_WINDOW);
            throw new UnauthorizedException(GENERIC_LOGIN_FAILURE);
        }

        rateLimiter.reset(addressKey);
        CreatedSession created = sessionStore.create(user.getId(), userAgent);
        return new LoginResult(user, created.rawToken(), created.session().getExpiresAt());
    }

    @Override
    public java.util.Optional<String> googleClientId() {
        return googleVerifier.isConfigured() ? java.util.Optional.of(googleVerifier.clientId()) : java.util.Optional.empty();
    }

    @Override
    @Transactional
    public LoginResult loginWithGoogle(String idToken, String userAgent) {
        GoogleIdentity identity = googleVerifier.verify(idToken);

        User user = userRepository.findByGoogleSubject(identity.subject()).orElse(null);
        if (user == null) {
            user = userRepository.findByEmail(identity.email()).orElse(null);
            if (user != null && user.getGoogleSubject() != null) {
                // The address belongs to an account tied to a different Google account; never hand it over.
                throw new UnauthorizedException("Google sign-in could not be verified. Please try again.");
            }
            if (user != null) {
                // First Google sign-in on an existing email account: Google has verified the address, so this is its owner.
                // linkGoogle drops any password and every earlier session is ended (see User.linkGoogle for why).
                user.linkGoogle(identity.subject());
                user = userRepository.save(user);
                revokeAllSessions(user.getId());
            } else {
                String name = identity.name().isBlank() ? identity.email().substring(0, identity.email().indexOf('@')) : identity.name();
                user = userRepository.save(User.fromGoogle(identity.email(), identity.subject(), name, "INR", "Asia/Kolkata"));
            }
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
