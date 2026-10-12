package com.finance.infrastructure.ratelimit;

import com.finance.application.exception.RateLimitedException;
import com.finance.application.ratelimit.RateDecision;
import com.finance.application.ratelimit.RateLimiter;
import com.finance.infrastructure.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.UUID;

/**
 * The request-rate rules (security.md: authentication, statement upload and every AI endpoint). It is an MVC interceptor
 * rather than a servlet filter on purpose: the browser only gets to read a 429's message when the CORS headers are on
 * it, and those are added by Spring MVC before interceptors run. It also sees unknown /api paths, so probing cannot dodge it.
 *
 * Per address: every API request counts toward a general ceiling, and sign-in, Google sign-in and password reset share a
 * tighter one (registration has its own). Per signed-in user: statement uploads and the AI endpoints, the costly ones.
 * Failed password guesses are counted separately inside the login itself, per account.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final String API = "/api/v1";

    private final RateLimiter limiter;
    private final ClientIpResolver ipResolver;
    private final TenantContext tenantContext;
    private final boolean enabled;
    private final int generalPerMinute;
    private final int authPerFiveMinutes;
    private final int registerPerHour;
    private final int uploadsPerHour;
    private final int chatPerMinute;
    private final int aiWritesPerMinute;
    private final int exportsPerHour;
    private final int deletesPerHour;
    private final int peerMultiplier;

    public RateLimitInterceptor(
            RateLimiter limiter,
            ClientIpResolver ipResolver,
            TenantContext tenantContext,
            @Value("${ratelimit.enabled:true}") boolean enabled,
            @Value("${ratelimit.general-per-minute:300}") int generalPerMinute,
            @Value("${ratelimit.auth-per-five-minutes:20}") int authPerFiveMinutes,
            @Value("${ratelimit.register-per-hour:10}") int registerPerHour,
            @Value("${ratelimit.uploads-per-hour:10}") int uploadsPerHour,
            @Value("${ratelimit.chat-per-minute:20}") int chatPerMinute,
            @Value("${ratelimit.ai-writes-per-minute:10}") int aiWritesPerMinute,
            @Value("${ratelimit.exports-per-hour:5}") int exportsPerHour,
            @Value("${ratelimit.deletes-per-hour:5}") int deletesPerHour,
            @Value("${ratelimit.peer-multiplier:10}") int peerMultiplier) {
        this.limiter = limiter;
        this.ipResolver = ipResolver;
        this.tenantContext = tenantContext;
        this.enabled = enabled;
        this.generalPerMinute = generalPerMinute;
        this.authPerFiveMinutes = authPerFiveMinutes;
        this.registerPerHour = registerPerHour;
        this.uploadsPerHour = uploadsPerHour;
        this.chatPerMinute = chatPerMinute;
        this.aiWritesPerMinute = aiWritesPerMinute;
        this.exportsPerHour = exportsPerHour;
        this.deletesPerHour = deletesPerHour;
        this.peerMultiplier = peerMultiplier;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || "OPTIONS".equals(request.getMethod())) {
            return true;
        }
        String ip = ipResolver.resolve(request);
        String peer = ipResolver.peerAddress(request);
        enforceAddress("general", ip, peer, generalPerMinute, Duration.ofMinutes(1));

        if ("GET".equals(request.getMethod()) && request.getRequestURI().equals(API + "/users/me/export")) {
            enforceForUser("export", exportsPerHour, Duration.ofHours(1));
        }
        if ("POST".equals(request.getMethod())) {
            String path = request.getRequestURI();
            switch (path) {
                case API + "/auth/login", API + "/auth/google", API + "/auth/password-reset", API + "/auth/password-reset/confirm" ->
                        enforceAddress("auth", ip, peer, authPerFiveMinutes, Duration.ofMinutes(5));
                case API + "/auth/register" -> enforceAddress("register", ip, peer, registerPerHour, Duration.ofHours(1));
                case API + "/users/me/delete" -> enforceForUser("delete", deletesPerHour, Duration.ofHours(1));
                case API + "/statements/upload" -> enforceForUser("upload", uploadsPerHour, Duration.ofHours(1));
                case API + "/ai/chat" -> enforceForUser("chat", chatPerMinute, Duration.ofMinutes(1));
                case API + "/ai/insights/monthly" -> enforceForUser("ai-write", aiWritesPerMinute, Duration.ofMinutes(1));
                default -> {
                    if (path.startsWith(API + "/statements/") && path.endsWith("/suggest-categories")) {
                        enforceForUser("ai-write", aiWritesPerMinute, Duration.ofMinutes(1));
                    }
                }
            }
        }
        return true;
    }

    /**
     * The strict limit is on the claimed client address. When a second proxy sits in front of us (the website forwarding calls
     * here) that claim is only as good as that proxy; a caller who skips it and talks to us directly can write anything into
     * it. So the address that actually connected to us also has a limit, {@code peerMultiplier} times higher because every
     * visitor behind the proxy shares it. With a single proxy the two addresses are the same and only the strict limit applies.
     */
    private void enforceAddress(String rule, String ip, String peer, int limit, Duration window) {
        enforce("ip-" + rule + ":" + ip, limit, window);
        if (!peer.equals(ip)) {
            enforce("peer-" + rule + ":" + peer, limit * peerMultiplier, window);
        }
    }

    private void enforceForUser(String rule, int limit, Duration window) {
        UUID userId = tenantContext.currentUserId();
        if (userId != null) { // an unauthenticated call is refused further on; nothing to count it against
            enforce("user-" + rule + ":" + userId, limit, window);
        }
    }

    private void enforce(String key, int limit, Duration window) {
        RateDecision decision = limiter.tryAcquire(key, limit, window);
        if (!decision.allowed()) {
            // The rule name only - never the address, account or anything else about the caller.
            log.warn("Rate limit hit: rule={}", key.substring(0, key.indexOf(':')));
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
    }
}
