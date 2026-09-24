package com.finance.auth;

import com.finance.support.ApiResult;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automates the curl-based verification this flow was originally hand-checked
 * with (see DECISIONS.md, Phase 1). Runs against real PostgreSQL (the `it`
 * profile) - RLS and Flyway's Postgres-native migrations don't work on H2.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class AuthFlowIT {

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenHasher tokenHasher;

    private TestHttpClient client;
    private String email;

    @BeforeEach
    void setUp() {
        client = new TestHttpClient(port);
        client.primeCsrfToken();
        email = TestData.uniqueEmail("auth");
    }

    @Test
    void registersLogsInAndReadsOwnProfile() {
        ApiResult register = client.post(
                "/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Auth Flow Test"));
        assertThat(register.status()).isEqualTo(200);
        assertThat(register.data().get("email").asText()).isEqualTo(email);

        ApiResult duplicateRegister = client.post(
                "/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Auth Flow Test"));
        assertThat(duplicateRegister.status()).isEqualTo(409);
        assertThat(duplicateRegister.errorCode()).isEqualTo("CONFLICT");

        ApiResult weakPassword = client.post(
                "/auth/register", Map.of("email", TestData.uniqueEmail("weak"), "password", "short", "fullName", "X"));
        assertThat(weakPassword.status()).isEqualTo(400);
        assertThat(weakPassword.errorCode()).isEqualTo("VALIDATION_ERROR");

        ApiResult wrongPassword = client.post("/auth/login", Map.of("email", email, "password", "wrongpassword"));
        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(wrongPassword.errorCode()).isEqualTo("UNAUTHORIZED");

        ApiResult unauthenticatedMe = client.get("/users/me");
        assertThat(unauthenticatedMe.status()).isEqualTo(401);

        ApiResult login = client.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        assertThat(login.status()).isEqualTo(200);
        String setCookie = login.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(setCookie)
                .as("session cookie attributes, per ADR-009")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax")
                .contains("Path=/");

        ApiResult me = client.get("/users/me");
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.data().get("email").asText()).isEqualTo(email);

        ApiResult sessions = client.get("/auth/sessions");
        assertThat(sessions.status()).isEqualTo(200);
        assertThat(sessions.data()).hasSize(1);

        ApiResult logout = client.post("/auth/logout", Map.of());
        assertThat(logout.status()).isEqualTo(204);

        ApiResult meAfterLogout = client.get("/users/me");
        assertThat(meAfterLogout.status()).isEqualTo(401);
    }

    @Test
    void csrfIsEnforcedOnMutatingRequests() {
        ApiResult withoutToken = client.postWithoutCsrf(
                "/auth/register",
                Map.of("email", TestData.uniqueEmail("csrf"), "password", PASSWORD, "fullName", "Csrf Test"));
        assertThat(withoutToken.status()).isEqualTo(403);
        assertThat(withoutToken.errorCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void revokeAllSessionsEndsAccess() {
        client.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Revoke Test"));
        client.post("/auth/login", Map.of("email", email, "password", PASSWORD));

        ApiResult revokeAll = client.delete("/auth/sessions");
        assertThat(revokeAll.status()).isEqualTo(204);

        ApiResult meAfterRevoke = client.get("/users/me");
        assertThat(meAfterRevoke.status()).isEqualTo(401);
    }

    @Test
    void passwordResetRequestNeverRevealsWhetherEmailExists() {
        client.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Reset Test"));

        ApiResult existing = client.post("/auth/password-reset", Map.of("email", email));
        ApiResult nonexistent = client.post("/auth/password-reset", Map.of("email", TestData.uniqueEmail("nobody")));

        assertThat(existing.status()).isEqualTo(202);
        assertThat(nonexistent.status()).isEqualTo(202);
    }

    @Test
    void passwordResetConfirmRejectsWrongTokenAndAcceptsCorrectOne() {
        client.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Reset Confirm Test"));
        client.post("/auth/password-reset", Map.of("email", email));

        ApiResult wrongToken = client.post(
                "/auth/password-reset/confirm", Map.of("token", "not-a-real-token", "password", "newpassword123"));
        assertThat(wrongToken.status()).isEqualTo(400);
        assertThat(wrongToken.errorCode()).isEqualTo("VALIDATION_ERROR");

        // The raw token is never returned by the API or logged, by design (see
        // LoggingEmailSender) - substitute a known raw token's hash directly,
        // the same workaround this flow was originally hand-verified with.
        String rawToken = "test-known-token-" + UUID.randomUUID();
        String tokenHash = tokenHasher.hash(rawToken);
        int updated = jdbcTemplate.update(
                "UPDATE password_reset_tokens SET token_hash = ? WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                tokenHash, email);
        assertThat(updated).isEqualTo(1);

        ApiResult confirm = client.post("/auth/password-reset/confirm", Map.of("token", rawToken, "password", "newpassword123"));
        assertThat(confirm.status()).isEqualTo(204);

        ApiResult loginOldPassword = client.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        assertThat(loginOldPassword.status()).isEqualTo(401);

        ApiResult loginNewPassword = client.post("/auth/login", Map.of("email", email, "password", "newpassword123"));
        assertThat(loginNewPassword.status()).isEqualTo(200);
    }
}
