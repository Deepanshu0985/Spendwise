package com.finance.infrastructure.web.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Off by default: every same-origin deployment so far (the Vite dev proxy locally, a
 * single edge reverse proxy in production per deployment.md) needs no CORS headers at
 * all, since the frontend and backend share one origin. A split-origin deployment
 * (frontend and backend on different domains, e.g. Vercel + Render) sets
 * app.cors.allowed-origin to the frontend's exact origin to allow its credentialed
 * fetch calls through. See security.cookie-samesite (application.properties) for the
 * matching cookie-side change a split-origin deployment also needs.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String allowedOrigin;

    public CorsConfig(@Value("${app.cors.allowed-origin:}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigin.isBlank()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-CSRF-Token")
                .allowCredentials(true);
    }
}
