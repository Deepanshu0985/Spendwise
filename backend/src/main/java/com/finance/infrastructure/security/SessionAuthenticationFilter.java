package com.finance.infrastructure.security;

import com.finance.infrastructure.tenancy.MutableTenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Resolves the session cookie into a user identity for the duration of the
 * request, then clears it - the ThreadLocal-backed TenantContext must never
 * survive past the request on a pooled Tomcat thread. Runs before
 * CsrfTokenFilter (lower @Order value = earlier).
 */
@Component
@Order(10)
public class SessionAuthenticationFilter extends HttpFilter {

    public static final String CURRENT_SESSION_ID_ATTRIBUTE = "com.finance.infrastructure.security.currentSessionId";

    private final SessionStore sessionStore;
    private final MutableTenantContext tenantContext;
    private final SessionCookieFactory cookieFactory;

    public SessionAuthenticationFilter(
            SessionStore sessionStore, MutableTenantContext tenantContext, SessionCookieFactory cookieFactory) {
        this.sessionStore = sessionStore;
        this.tenantContext = tenantContext;
        this.cookieFactory = cookieFactory;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            findCookieValue(request)
                    .flatMap(sessionStore::resolve)
                    .ifPresent(session -> {
                        tenantContext.setCurrentUserId(session.getUserId());
                        request.setAttribute(CURRENT_SESSION_ID_ATTRIBUTE, session.getId());
                        sessionStore.touch(session.getId());
                    });
            chain.doFilter(request, response);
        } finally {
            tenantContext.clear();
        }
    }

    private Optional<String> findCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(cookieFactory.cookieName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
