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
            @Value("${ratelimit.ai-writes-per-minute:10}") int aiWritesPerMinute) {
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
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || "OPTIONS".equals(request.getMethod())) {
            return true;
        }
        String ip = ipResolver.resolve(request);
        enforce("ip-general:" + ip, generalPerMinute, Duration.ofMinutes(1));

        if ("POST".equals(request.getMethod())) {
            String path = request.getRequestURI();
            switch (path) {
                case API + "/auth/login", API + "/auth/google", API + "/auth/password-reset", API + "/auth/password-reset/confirm" ->
                        enforce("ip-auth:" + ip, authPerFiveMinutes, Duration.ofMinutes(5));
                case API + "/auth/register" -> enforce("ip-register:" + ip, registerPerHour, Duration.ofHours(1));
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
