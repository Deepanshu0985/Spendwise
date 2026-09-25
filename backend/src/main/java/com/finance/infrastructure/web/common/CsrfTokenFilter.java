package com.finance.infrastructure.web.common;

import com.finance.application.exception.ApiError;
import com.finance.application.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Double-submit cookie CSRF defense, alongside SameSite=Lax (ADR-009). Every
 * response ensures a non-httpOnly CSRF cookie exists so the frontend can read
 * and echo it back; every state-changing request must send it back as a header
 * matching the cookie, or is rejected. Runs after SessionAuthenticationFilter.
 */
@Component
@Order(20)
public class CsrfTokenFilter extends HttpFilter {

    private static final String COOKIE_NAME = "csrf_token";
    private static final String HEADER_NAME = "X-CSRF-Token";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final SecureRandom secureRandom = new SecureRandom();
    private final ObjectMapper objectMapper;

    public CsrfTokenFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String existingToken = findCookieValue(request).orElse(null);

        // Filters run before DispatcherServlet, so @RestControllerAdvice never sees
        // exceptions thrown from here - the response must be written directly.
        if (!SAFE_METHODS.contains(request.getMethod())) {
            String header = request.getHeader(HEADER_NAME);
            if (existingToken == null || header == null || !existingToken.equals(header)) {
                writeForbidden(response);
                return;
            }
        }

        if (existingToken == null) {
            String newToken = generateToken();
            response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, buildCookie(newToken).toString());
        }

        chain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        ApiError error = new ApiError(
                ErrorCode.FORBIDDEN.name(), "Missing or invalid CSRF token.", List.of(), UUID.randomUUID().toString());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ApiError.Envelope(error)));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ResponseCookie buildCookie(String token) {
        // Not httpOnly: the frontend must be able to read this to echo it back as a header.
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(false)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .build();
    }

    private Optional<String> findCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(COOKIE_NAME))
                .map(Cookie::getValue)
                .findFirst();
    }
}
