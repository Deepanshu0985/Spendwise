package com.finance.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * One instance = one browser session (one user), with its own isolated cookie
 * jar - mirrors the curl-with-a-cookie-jar approach this project's auth/RLS
 * behavior was originally hand-verified with (see DECISIONS.md), just
 * automated. Real HTTP over a real embedded Tomcat on a random port, so the
 * whole filter chain (SessionAuthenticationFilter, CsrfTokenFilter) runs for
 * real, not a mocked approximation of it.
 *
 * Cookies are tracked manually rather than via java.net.CookieManager: our
 * cookies are Secure-flagged (correctly, for production), and the JDK's
 * CookieManager will not resend a Secure cookie over the plain http://
 * connection this test server uses (no TLS in tests) - curl has no such
 * restriction, which is why the original hand verification worked and a
 * CookieManager-based version of this helper silently didn't (every mutating
 * call failed CSRF because the cookie was never sent back - caught by running
 * this against a real server before trusting it, not just reading the code).
 */
public final class TestHttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client = HttpClient.newHttpClient();
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private final String baseUrl;

    public TestHttpClient(int port) {
        this.baseUrl = "http://localhost:" + port + "/api/v1";
    }

    /** Every session must call this once before any mutating call, to mint the CSRF cookie - mirrors real frontend behavior. */
    public ApiResult primeCsrfToken() {
        return get("/health");
    }

    public ApiResult get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET());
    }

    public ApiResult post(String path, Object body) {
        return send(withCsrf(HttpRequest.newBuilder(uri(path))).POST(bodyPublisher(body)));
    }

    public ApiResult put(String path, Object body) {
        return send(withCsrf(HttpRequest.newBuilder(uri(path))).PUT(bodyPublisher(body)));
    }

    public ApiResult delete(String path) {
        return send(withCsrf(HttpRequest.newBuilder(uri(path))).DELETE());
    }

    /** For deliberately testing CSRF rejection: a mutating call with no X-CSRF-Token header at all. */
    public ApiResult postWithoutCsrf(String path, Object body) {
        return send(withCookiesOnly(HttpRequest.newBuilder(uri(path)))
                .header("Content-Type", "application/json")
                .POST(bodyPublisher(body)));
    }

    private HttpRequest.Builder withCsrf(HttpRequest.Builder builder) {
        withCookiesOnly(builder).header("Content-Type", "application/json");
        if (cookies.containsKey("csrf_token")) {
            builder.header("X-CSRF-Token", cookies.get("csrf_token"));
        }
        return builder;
    }

    private HttpRequest.Builder withCookiesOnly(HttpRequest.Builder builder) {
        if (!cookies.isEmpty()) {
            builder.header("Cookie", cookies.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("; ")));
        }
        return builder;
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    private HttpRequest.BodyPublisher bodyPublisher(Object body) {
        try {
            return HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not serialize test request body", e);
        }
    }

    private ApiResult send(HttpRequest.Builder builder) {
        try {
            withCookiesOnly(builder);
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            captureCookies(response.headers().allValues("Set-Cookie"));
            String raw = response.body();
            JsonNode json = (raw == null || raw.isBlank()) ? NullNode.instance : MAPPER.readTree(raw);
            return new ApiResult(response.statusCode(), response.headers(), json);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Test HTTP call failed", e);
        }
    }

    private void captureCookies(List<String> setCookieHeaders) {
        for (String header : setCookieHeaders) {
            String firstPair = header.split(";", 2)[0];
            int eq = firstPair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String name = firstPair.substring(0, eq).trim();
            String value = firstPair.substring(eq + 1).trim();
            cookies.put(name, value);
        }
    }

    public Optional<String> cookie(String name) {
        return Optional.ofNullable(cookies.get(name));
    }
}
