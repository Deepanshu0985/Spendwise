package com.finance.auth;

import com.finance.infrastructure.security.TokenHasher;
import com.finance.support.ApiResult;
import com.finance.support.FakeGoogleIdentityVerifier;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Sign in with Google end to end, with a stand-in for Google's token check (the real check is covered by GoogleIdTokenVerifierTest). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Import(GoogleLoginIT.FakeGoogleConfig.class)
class GoogleLoginIT {

    @TestConfiguration
    static class FakeGoogleConfig {
        @Bean
        @Primary
        FakeGoogleIdentityVerifier fakeGoogleIdentityVerifier() {
            return new FakeGoogleIdentityVerifier();
        }
    }

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    @Autowired
    private FakeGoogleIdentityVerifier fakeGoogle;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenHasher tokenHasher;

    private TestHttpClient client;
    private String email;
    private String subject;

    @BeforeEach
    void setUp() {
        fakeGoogle.configured = true;
        client = new TestHttpClient(port);
        client.primeCsrfToken();
        email = TestData.uniqueEmail("google");
        subject = "sub-" + UUID.randomUUID();
    }

    @Test
    void aNewGoogleUserGetsAnAccountAndASession() {
        String token = fakeGoogle.tokenFor(subject, email, "Gina Google");

        ApiResult login = client.post("/auth/google", Map.of("credential", token));
        assertThat(login.status()).isEqualTo(200);
        assertThat(login.data().get("email").asText()).isEqualTo(email);
        assertThat(login.data().get("fullName").asText()).isEqualTo("Gina Google");
        assertThat(login.headers().firstValue("Set-Cookie").orElseThrow()).contains("HttpOnly").contains("Secure");

        assertThat(client.get("/users/me").status()).isEqualTo(200);

        // The stored row has no password and carries Google's account id.
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT password_hash, google_subject FROM users WHERE email = ?", email);
        assertThat(row.get("password_hash")).isNull();
        assertThat(row.get("google_subject")).isEqualTo(subject);
    }

    @Test
    void signingInAgainReachesTheSameAccount() {
        String token = fakeGoogle.tokenFor(subject, email, "Gina Google");
        client.post("/auth/google", Map.of("credential", token));
        client.post("/auth/logout", Map.of());

        TestHttpClient second = new TestHttpClient(port);
        second.primeCsrfToken();
        assertThat(second.post("/auth/google", Map.of("credential", token)).status()).isEqualTo(200);

        Integer accounts = jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email);
        assertThat(accounts).isEqualTo(1);
    }

    @Test
    void anExistingPasswordAccountIsLinkedAndItsPasswordAndSessionsAreDropped() {
        client.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Pat Password"));
        assertThat(client.post("/auth/login", Map.of("email", email, "password", PASSWORD)).status()).isEqualTo(200);
        assertThat(client.get("/users/me").status()).isEqualTo(200);

        // The same person (or whoever first registered the address) is now signed in through Google, from another browser.
        TestHttpClient other = new TestHttpClient(port);
        other.primeCsrfToken();
        ApiResult google = other.post("/auth/google", Map.of("credential", fakeGoogle.tokenFor(subject, email, "Pat Real")));
        assertThat(google.status()).isEqualTo(200);

        Integer accounts = jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email);
        assertThat(accounts).as("linked, not duplicated").isEqualTo(1);

        // Whoever held the old password or session no longer gets in.
        assertThat(client.get("/users/me").status()).as("earlier session ended").isEqualTo(401);
        assertThat(client.post("/auth/login", Map.of("email", email, "password", PASSWORD)).status())
                .as("old password dropped")
                .isEqualTo(401);
        assertThat(other.get("/users/me").status()).isEqualTo(200);
    }

    @Test
    void anAccountTiedToOneGoogleAccountCannotBeTakenOverByAnother() {
        client.post("/auth/google", Map.of("credential", fakeGoogle.tokenFor(subject, email, "First Owner")));

        TestHttpClient other = new TestHttpClient(port);
        other.primeCsrfToken();
        ApiResult takeover = other.post("/auth/google", Map.of("credential", fakeGoogle.tokenFor("sub-" + UUID.randomUUID(), email, "Someone Else")));
        assertThat(takeover.status()).isEqualTo(401);
        assertThat(other.get("/users/me").status()).isEqualTo(401);
    }

    @Test
    void theLoginPageCanLearnWhetherAndHowToOfferGoogle() {
        ApiResult on = client.get("/auth/google/config");
        assertThat(on.status()).isEqualTo(200);
        assertThat(on.data().get("enabled").asBoolean()).isTrue();
        assertThat(on.data().get("clientId").asText()).isEqualTo("fake-client-id.apps.googleusercontent.com");

        fakeGoogle.configured = false;
        ApiResult off = client.get("/auth/google/config");
        assertThat(off.status()).isEqualTo(200);
        assertThat(off.data().get("enabled").asBoolean()).isFalse();
    }

    @Test
    void aTokenGoogleDidNotIssueIsRefused() {
        ApiResult forged = client.post("/auth/google", Map.of("credential", "forged-token"));
        assertThat(forged.status()).isEqualTo(401);
        assertThat(forged.errorCode()).isEqualTo("UNAUTHORIZED");
        assertThat(client.get("/users/me").status()).isEqualTo(401);

        assertThat(client.post("/auth/google", Map.of("credential", "")).status()).isEqualTo(400);
        assertThat(client.post("/auth/google", Map.of()).status()).isEqualTo(400);
    }

    @Test
    void googleSignInIsReportedUnavailableWhenNotConfigured() {
        fakeGoogle.configured = false;
        ApiResult result = client.post("/auth/google", Map.of("credential", fakeGoogle.tokenFor(subject, email, "X")));
        assertThat(result.status()).isEqualTo(503);
        assertThat(result.errorCode()).isEqualTo("FEATURE_UNAVAILABLE");
    }

    @Test
    void aGoogleOnlyAccountCannotUsePasswordLoginButCanSetAPasswordThroughReset() {
        client.post("/auth/google", Map.of("credential", fakeGoogle.tokenFor(subject, email, "Gina Google")));
        client.post("/auth/logout", Map.of());

        ApiResult passwordLogin = client.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        assertThat(passwordLogin.status()).isEqualTo(401);
        assertThat(passwordLogin.errorCode()).isEqualTo("UNAUTHORIZED");

        client.post("/auth/password-reset", Map.of("email", email));
        String rawToken = "test-known-token-" + UUID.randomUUID();
        jdbcTemplate.update(
                "UPDATE password_reset_tokens SET token_hash = ? WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                tokenHasher.hash(rawToken), email);
        assertThat(client.post("/auth/password-reset/confirm", Map.of("token", rawToken, "password", "newpassword123")).status())
                .isEqualTo(204);

        assertThat(client.post("/auth/login", Map.of("email", email, "password", "newpassword123")).status()).isEqualTo(200);
    }
}
