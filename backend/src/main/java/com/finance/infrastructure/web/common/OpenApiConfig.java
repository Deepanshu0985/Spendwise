package com.finance.infrastructure.web.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui/index.html, generated from the existing controller
 * annotations - no extra doc-comment burden. Declares the double-submit CSRF
 * header (ADR-009) as a security scheme so "Authorize" in the UI attaches it
 * to every "Try it out" call, the same header CsrfTokenFilter checks for real.
 * Disabled in production (application-prod.properties) - not meant to be
 * publicly exposed once deployed.
 */
@Configuration
public class OpenApiConfig {

    private static final String CSRF_SCHEME = "csrfToken";

    @Bean
    public OpenAPI spendwiseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spendwise API")
                        .version("v1")
                        .description(
                                "Personal Finance Intelligence App backend. Authenticate via POST "
                                        + "/auth/login first (sets the session cookie), then click "
                                        + "Authorize below and paste the csrf_token cookie's value - "
                                        + "GET /health sets that cookie if you don't have one yet."))
                .components(new Components()
                        .addSecuritySchemes(
                                CSRF_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-CSRF-Token")))
                .addSecurityItem(new SecurityRequirement().addList(CSRF_SCHEME));
    }
}
