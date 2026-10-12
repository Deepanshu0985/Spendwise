package com.finance.infrastructure.ops;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Outermost filter, so it sees the final status of every API request, including ones that fail before reaching a controller. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestMetricsFilter extends OncePerRequestFilter {

    private final RequestMetrics metrics;

    public RequestMetricsFilter(RequestMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.equals("/api/v1/health"); // the keep-awake pings would drown the real traffic
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        boolean signIn = "POST".equals(request.getMethod()) && request.getRequestURI().matches("/api/v1/auth/(login|google)");
        try {
            chain.doFilter(request, response);
            metrics.record(response.getStatus(), (System.nanoTime() - started) / 1_000_000, signIn);
        } catch (IOException | ServletException | RuntimeException e) {
            metrics.record(500, (System.nanoTime() - started) / 1_000_000, signIn);
            throw e;
        }
    }
}
