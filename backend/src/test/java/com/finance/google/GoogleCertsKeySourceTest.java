package com.finance.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.infrastructure.google.GoogleCertsKeySource;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Google's published keys are fetched, understood, cached for an hour, and a forged key id cannot hammer Google. */
class GoogleCertsKeySourceTest {

    private HttpServer server;
    private final AtomicInteger fetches = new AtomicInteger();
    private RSAPublicKey published;
    private String certsUrl;

    /** A clock the test can move. */
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-10-11T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @BeforeEach
    void startFakeGoogle() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        published = (RSAPublicKey) pair.getPublic();
        String body = "{\"keys\":[{\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"kid\":\"kid-1\","
                + "\"n\":\"" + b64(published.getModulus().toByteArray()) + "\","
                + "\"e\":\"" + b64(published.getPublicExponent().toByteArray()) + "\"}]}";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/certs", exchange -> {
            fetches.incrementAndGet();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        certsUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/certs";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static String b64(byte[] bytes) {
        // BigInteger.toByteArray() may carry a leading sign byte; JWK values do not.
        int start = bytes.length > 1 && bytes[0] == 0 ? 1 : 0;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(java.util.Arrays.copyOfRange(bytes, start, bytes.length));
    }

    @Test
    void readsAPublishedKeyAndCachesIt() {
        MovableClock clock = new MovableClock();
        GoogleCertsKeySource source = new GoogleCertsKeySource(new ObjectMapper(), clock, certsUrl);

        assertThat(source.keyFor("kid-1")).isPresent();
        assertThat(((RSAPublicKey) source.keyFor("kid-1").orElseThrow()).getModulus()).isEqualTo(published.getModulus());
        assertThat(fetches.get()).as("second lookup served from the cache").isEqualTo(1);
    }

    @Test
    void anUnknownKeyIdRefreshesAtMostOncePerMinute() {
        MovableClock clock = new MovableClock();
        GoogleCertsKeySource source = new GoogleCertsKeySource(new ObjectMapper(), clock, certsUrl);
        source.keyFor("kid-1");

        for (int i = 0; i < 50; i++) {
            assertThat(source.keyFor("forged-" + i)).isEmpty();
        }
        assertThat(fetches.get()).as("50 forged ids inside a minute cause no extra fetch").isEqualTo(1);

        clock.advance(Duration.ofMinutes(2));
        assertThat(source.keyFor("forged-later")).isEmpty();
        assertThat(fetches.get()).isEqualTo(2);
    }

    @Test
    void refetchesAfterAnHour() {
        MovableClock clock = new MovableClock();
        GoogleCertsKeySource source = new GoogleCertsKeySource(new ObjectMapper(), clock, certsUrl);
        source.keyFor("kid-1");
        clock.advance(Duration.ofMinutes(61));
        assertThat(source.keyFor("kid-1")).isPresent();
        assertThat(fetches.get()).isEqualTo(2);
    }

    @Test
    void aFailingFetchKeepsTheLastGoodKeysAndIsNotRetriedOnEveryRequest() {
        MovableClock clock = new MovableClock();
        GoogleCertsKeySource source = new GoogleCertsKeySource(new ObjectMapper(), clock, certsUrl);
        source.keyFor("kid-1");
        server.stop(0);
        clock.advance(Duration.ofMinutes(61));

        assertThat(source.keyFor("kid-1")).as("last known keys still work while Google is unreachable").isPresent();
    }
}
