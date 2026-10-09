package com.finance.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The browser's cross-site permission check (preflight) for a split-origin deployment, e.g. a Vercel
 * frontend calling a Render backend. Every request header the frontend sends must be listed in
 * Access-Control-Allow-Headers or the browser blocks the call before it is made - which the
 * same-origin Vite dev proxy hides completely, so this has to be tested explicitly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.cors.allowed-origin=https://frontend.example.test")
@ActiveProfiles("it")
class CorsIT {

    private static final String FRONTEND = "https://frontend.example.test";

    @LocalServerPort
    private int port;

    private HttpResponse<String> preflight(String origin, String requestHeaders) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/transactions"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", requestHeaders)
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void preflightAllowsEveryHeaderTheFrontendSendsIncludingTheIdempotencyKey() throws Exception {
        HttpResponse<String> response = preflight(FRONTEND, "content-type,x-csrf-token,idempotency-key");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(FRONTEND);
        assertThat(response.headers().firstValue("Access-Control-Allow-Headers").orElse("").toLowerCase())
                .contains("idempotency-key", "x-csrf-token", "content-type");
    }

    @Test
    void preflightFromAnyOtherOriginIsRefused() throws Exception {
        HttpResponse<String> response = preflight("https://evil.example.test", "content-type");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }
}
