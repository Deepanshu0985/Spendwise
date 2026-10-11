package com.finance.infrastructure.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.net.http.HttpClient;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Google publishes the public keys its ID tokens are signed with and rotates them every few days. They are cached for
 * an hour; a token signed with a key not in the cache triggers one refresh, but never more than once a minute, so a
 * flood of forged tokens cannot turn into a flood of requests to Google.
 */
@Component
public class GoogleCertsKeySource implements GoogleKeySource {

    private static final Logger log = LoggerFactory.getLogger(GoogleCertsKeySource.class);
    private static final Duration MAX_AGE = Duration.ofHours(1);
    private static final Duration MIN_REFRESH_GAP = Duration.ofMinutes(1);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String certsUrl;

    private Map<String, PublicKey> keys = Map.of();
    private Instant fetchedAt = Instant.MIN;

    public GoogleCertsKeySource(
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${google.certs-url:https://www.googleapis.com/oauth2/v3/certs}") String certsUrl) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.certsUrl = certsUrl;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public synchronized Optional<PublicKey> keyFor(String keyId) {
        Instant now = clock.instant();
        boolean stale = Duration.between(fetchedAt, now).compareTo(MAX_AGE) > 0;
        boolean unknownAndMayRefresh = !keys.containsKey(keyId) && Duration.between(fetchedAt, now).compareTo(MIN_REFRESH_GAP) > 0;
        if (stale || unknownAndMayRefresh) {
            refresh(now);
        }
        return Optional.ofNullable(keys.get(keyId));
    }

    private void refresh(Instant now) {
        fetchedAt = now; // also on failure: a down Google must not be retried on every request
        try {
            String body = restClient.get().uri(certsUrl).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);
            Map<String, PublicKey> loaded = new HashMap<>();
            KeyFactory factory = KeyFactory.getInstance("RSA");
            for (JsonNode key : root.path("keys")) {
                if (!"RSA".equals(key.path("kty").asText())) {
                    continue;
                }
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("n").asText()));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("e").asText()));
                loaded.put(key.path("kid").asText(), factory.generatePublic(new RSAPublicKeySpec(modulus, exponent)));
            }
            if (!loaded.isEmpty()) {
                keys = Map.copyOf(loaded);
            }
        } catch (Exception e) {
            log.warn("Could not refresh Google's signing keys: {}", e.getClass().getSimpleName());
        }
    }
}
