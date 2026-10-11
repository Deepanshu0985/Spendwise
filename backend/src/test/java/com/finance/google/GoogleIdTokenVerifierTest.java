package com.finance.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.application.auth.GoogleIdentity;
import com.finance.application.exception.UnauthorizedException;
import com.finance.infrastructure.google.GoogleIdTokenVerifier;
import com.finance.infrastructure.google.GoogleKeySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "my-app.apps.googleusercontent.com";
    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");

    private final ObjectMapper mapper = new ObjectMapper();
    private KeyPair googleKey;
    private KeyPair strangerKey;
    private GoogleIdTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        googleKey = generator.generateKeyPair();
        strangerKey = generator.generateKeyPair();
        GoogleKeySource keys = keyId -> "kid-1".equals(keyId) ? Optional.<PublicKey>of(googleKey.getPublic()) : Optional.empty();
        verifier = new GoogleIdTokenVerifier(CLIENT_ID, keys, mapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Map<String, Object> goodClaims() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "https://accounts.google.com");
        claims.put("aud", CLIENT_ID);
        claims.put("sub", "1234567890");
        claims.put("email", "Person@Example.com");
        claims.put("email_verified", true);
        claims.put("name", "Pat Person");
        claims.put("iat", NOW.getEpochSecond() - 10);
        claims.put("exp", NOW.getEpochSecond() + 3600);
        return claims;
    }

    private String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String token(Map<String, Object> header, Map<String, Object> claims, KeyPair signer) throws Exception {
        String signingInput = b64(mapper.writeValueAsBytes(header)) + "." + b64(mapper.writeValueAsBytes(claims));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(signer.getPrivate());
        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + b64(signature.sign());
    }

    private String token(Map<String, Object> claims) throws Exception {
        return token(Map.of("alg", "RS256", "kid", "kid-1"), claims, googleKey);
    }

    private void assertRejected(String token) {
        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void acceptsAValidTokenAndLowercasesTheEmail() throws Exception {
        GoogleIdentity identity = verifier.verify(token(goodClaims()));
        assertThat(identity.subject()).isEqualTo("1234567890");
        assertThat(identity.email()).isEqualTo("person@example.com");
        assertThat(identity.name()).isEqualTo("Pat Person");
    }

    @Test
    void acceptsTheBareIssuerAndAnAudienceList() throws Exception {
        Map<String, Object> claims = goodClaims();
        claims.put("iss", "accounts.google.com");
        claims.put("aud", java.util.List.of("other", CLIENT_ID));
        assertThat(verifier.verify(token(claims)).subject()).isEqualTo("1234567890");
    }

    @Test
    void rejectsATokenMintedForAnotherApp() throws Exception {
        Map<String, Object> claims = goodClaims();
        claims.put("aud", "someone-elses-app.apps.googleusercontent.com");
        assertRejected(token(claims));
    }

    @Test
    void rejectsAnExpiredTokenButToleratesASecondsOfClockSkew() throws Exception {
        Map<String, Object> expired = goodClaims();
        expired.put("exp", NOW.getEpochSecond() - 120);
        assertRejected(token(expired));

        Map<String, Object> justExpired = goodClaims();
        justExpired.put("exp", NOW.getEpochSecond() - 30);
        assertThat(verifier.verify(token(justExpired))).isNotNull();
    }

    @Test
    void rejectsATokenIssuedInTheFuture() throws Exception {
        Map<String, Object> claims = goodClaims();
        claims.put("iat", NOW.getEpochSecond() + 600);
        assertRejected(token(claims));
    }

    @Test
    void rejectsTheWrongIssuer() throws Exception {
        Map<String, Object> claims = goodClaims();
        claims.put("iss", "https://evil.example.com");
        assertRejected(token(claims));
    }

    @Test
    void rejectsAnEmailGoogleHasNotVerified() throws Exception {
        Map<String, Object> claims = goodClaims();
        claims.put("email_verified", false);
        assertRejected(token(claims));
        claims.remove("email_verified");
        assertRejected(token(claims));
    }

    @Test
    void rejectsATokenSignedWithAKeyGoogleDidNotPublish() throws Exception {
        assertRejected(token(Map.of("alg", "RS256", "kid", "kid-1"), goodClaims(), strangerKey));
    }

    @Test
    void rejectsAnUnknownKeyId() throws Exception {
        assertRejected(token(Map.of("alg", "RS256", "kid", "kid-unknown"), goodClaims(), googleKey));
    }

    @Test
    void rejectsATokenWhoseClaimsWereChangedAfterSigning() throws Exception {
        String genuine = token(goodClaims());
        Map<String, Object> forged = goodClaims();
        forged.put("email", "victim@example.com");
        String[] parts = genuine.split("\\.");
        assertRejected(parts[0] + "." + b64(mapper.writeValueAsBytes(forged)) + "." + parts[2]);
    }

    @Test
    void rejectsUnsignedAndNonRs256Tokens() throws Exception {
        String none = b64(mapper.writeValueAsBytes(Map.of("alg", "none", "kid", "kid-1")))
                + "." + b64(mapper.writeValueAsBytes(goodClaims())) + ".";
        assertRejected(none);

        String hs256 = b64(mapper.writeValueAsBytes(Map.of("alg", "HS256", "kid", "kid-1")))
                + "." + b64(mapper.writeValueAsBytes(goodClaims())) + "." + b64("anything".getBytes(StandardCharsets.UTF_8));
        assertRejected(hs256);
    }

    @Test
    void rejectsGarbage() {
        assertRejected(null);
        assertRejected("");
        assertRejected("not-a-token");
        assertRejected("a.b.c");
        assertRejected("x".repeat(9000));
    }

    @Test
    void refusesEverythingWhenNoClientIdIsConfigured() throws Exception {
        GoogleIdTokenVerifier unconfigured =
                new GoogleIdTokenVerifier("", keyId -> Optional.of(googleKey.getPublic()), mapper, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(unconfigured.isConfigured()).isFalse();
        String valid = token(goodClaims());
        assertThatThrownBy(() -> unconfigured.verify(valid)).isInstanceOf(UnauthorizedException.class);
    }
}
