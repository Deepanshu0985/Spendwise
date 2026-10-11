package com.finance.infrastructure.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.application.auth.GoogleIdentity;
import com.finance.application.auth.GoogleIdentityVerifier;
import com.finance.application.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.util.Base64;
import java.util.Set;

/**
 * Checks a Google ID token the way Google's documentation requires: an RS256 signature from one of Google's published
 * keys, the issuer, that the token was minted for THIS app's client id (so a token Google issued to some other app is
 * useless here), that it has not expired, and that Google has verified the email. Only the header's algorithm RS256 is
 * accepted - never "none" or a symmetric algorithm - so a token cannot pick its own weaker check. Nothing about a
 * rejected token is revealed: every failure is the same message.
 */
@Component
public class GoogleIdTokenVerifier implements GoogleIdentityVerifier {

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final long CLOCK_SKEW_SECONDS = 60;
    private static final int MAX_TOKEN_LENGTH = 8192;
    private static final String REJECTED = "Google sign-in could not be verified. Please try again.";

    private final String clientId;
    private final GoogleKeySource keySource;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public GoogleIdTokenVerifier(
            @Value("${google.client-id:}") String clientId, GoogleKeySource keySource, ObjectMapper objectMapper, Clock clock) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.keySource = keySource;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public boolean isConfigured() {
        return !clientId.isEmpty();
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (!isConfigured() || idToken == null || idToken.length() > MAX_TOKEN_LENGTH) {
            throw new UnauthorizedException(REJECTED);
        }
        String[] parts = idToken.split("\\.", -1);
        if (parts.length != 3) {
            throw new UnauthorizedException(REJECTED);
        }
        try {
            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if (!"RS256".equals(header.path("alg").asText())) {
                throw new UnauthorizedException(REJECTED);
            }
            PublicKey key = keySource.keyFor(header.path("kid").asText()).orElseThrow(() -> new UnauthorizedException(REJECTED));

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!signature.verify(Base64.getUrlDecoder().decode(parts[2]))) {
                throw new UnauthorizedException(REJECTED);
            }

            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            long now = clock.instant().getEpochSecond();
            if (!ISSUERS.contains(claims.path("iss").asText())
                    || !audienceMatches(claims.path("aud"))
                    || !claims.path("exp").canConvertToLong()
                    || claims.path("exp").asLong() + CLOCK_SKEW_SECONDS < now
                    || (claims.has("iat") && claims.path("iat").asLong() - CLOCK_SKEW_SECONDS > now)) {
                throw new UnauthorizedException(REJECTED);
            }
            String subject = claims.path("sub").asText("");
            String email = claims.path("email").asText("").strip().toLowerCase();
            boolean emailVerified = claims.path("email_verified").asBoolean(false);
            if (subject.isEmpty() || email.isEmpty() || !emailVerified) {
                throw new UnauthorizedException(REJECTED);
            }
            return new GoogleIdentity(subject, email, true, claims.path("name").asText(""));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException(REJECTED);
        }
    }

    private boolean audienceMatches(JsonNode audience) {
        if (audience.isTextual()) {
            return clientId.equals(audience.asText());
        }
        if (audience.isArray()) {
            for (JsonNode entry : audience) {
                if (clientId.equals(entry.asText())) {
                    return true;
                }
            }
        }
        return false;
    }
}
