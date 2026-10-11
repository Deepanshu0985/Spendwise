package com.finance.ratelimit;

import com.finance.support.ApiResult;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rate rules through the real HTTP stack, with small limits so they can be reached. Every test comes from its own
 * made-up address (X-Forwarded-For with one trusted proxy hop) so the counters of one test never touch another's.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestPropertySource(properties = {
        "ratelimit.enabled=true",
        "ratelimit.trusted-proxy-hops=1",
        "ratelimit.general-per-minute=40",
        "ratelimit.auth-per-five-minutes=12",
        "ratelimit.register-per-hour=3",
        "ratelimit.uploads-per-hour=2",
        "app.cors.allowed-origin=https://app.example.com"
})
class RateLimitIT {

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    private static String newAddress() {
        int a = (int) (Math.random() * 200) + 20;
        return "198.51." + a + "." + ((int) (Math.random() * 250) + 1);
    }

    private TestHttpClient clientAt(String address) {
        TestHttpClient client = new TestHttpClient(port).withHeader("X-Forwarded-For", address);
        client.primeCsrfToken();
        return client;
    }

    private String registerAt(TestHttpClient client) {
        String email = TestData.uniqueEmail("rl");
        assertThat(client.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Rate Limit")).status()).isEqualTo(200);
        return email;
    }

    @Test
    void repeatedWrongPasswordsLockThatAddressAndAccountOutEvenWithTheRightPassword() {
        TestHttpClient client = clientAt(newAddress());
        String email = registerAt(client);

        for (int i = 0; i < 5; i++) {
            assertThat(client.post("/auth/login", Map.of("email", email, "password", "wrong-password-" + i)).status()).isEqualTo(401);
        }
        ApiResult locked = client.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        assertThat(locked.status()).as("the right password is refused while locked").isEqualTo(429);
        assertThat(locked.errorCode()).isEqualTo("RATE_LIMITED");
        assertThat(locked.errorMessage()).contains("Please wait");
        assertThat(Long.parseLong(locked.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 900L);
    }

    @Test
    void anUnknownEmailIsLockedTheSameWayAsAKnownOneSoNothingIsRevealed() {
        TestHttpClient client = clientAt(newAddress());
        String ghost = TestData.uniqueEmail("ghost");
        for (int i = 0; i < 5; i++) {
            assertThat(client.post("/auth/login", Map.of("email", ghost, "password", "whatever" + i)).status()).isEqualTo(401);
        }
        assertThat(client.post("/auth/login", Map.of("email", ghost, "password", "whatever")).status()).isEqualTo(429);
    }

    @Test
    void aStrangersFailuresDoNotLockTheOwnerOutFromTheirOwnAddress() {
        String owner = newAddress();
        TestHttpClient ownerClient = clientAt(owner);
        String email = registerAt(ownerClient);

        TestHttpClient attacker = clientAt(newAddress());
        for (int i = 0; i < 6; i++) {
            attacker.post("/auth/login", Map.of("email", email, "password", "guess" + i));
        }
        assertThat(attacker.post("/auth/login", Map.of("email", email, "password", PASSWORD)).status()).isEqualTo(429);

        assertThat(ownerClient.post("/auth/login", Map.of("email", email, "password", PASSWORD)).status()).isEqualTo(200);
    }

    @Test
    void aSuccessfulLoginClearsTheFailureCount() {
        TestHttpClient client = clientAt(newAddress());
        String email = registerAt(client);
        for (int i = 0; i < 4; i++) {
            client.post("/auth/login", Map.of("email", email, "password", "nope" + i));
        }
        assertThat(client.post("/auth/login", Map.of("email", email, "password", PASSWORD)).status()).isEqualTo(200);
        for (int i = 0; i < 4; i++) {
            assertThat(client.post("/auth/login", Map.of("email", email, "password", "again" + i)).status()).isEqualTo(401);
        }
    }

    @Test
    void anAddressThatKeepsHittingSignInIsStoppedNoMatterWhichEmailsItTries() {
        TestHttpClient client = clientAt(newAddress());
        int refused = 0;
        for (int i = 0; i < 16; i++) {
            ApiResult result = client.post("/auth/login", Map.of("email", TestData.uniqueEmail("spray"), "password", "x" + i));
            if (result.status() == 429) {
                refused++;
            }
        }
        assertThat(refused).as("12 allowed in five minutes, the rest refused").isEqualTo(4);
    }

    @Test
    void registrationIsLimitedPerAddress() {
        TestHttpClient client = clientAt(newAddress());
        for (int i = 0; i < 3; i++) {
            registerAt(client);
        }
        ApiResult fourth = client.post("/auth/register", Map.of("email", TestData.uniqueEmail("rl"), "password", PASSWORD, "fullName", "Too Many"));
        assertThat(fourth.status()).isEqualTo(429);
    }

    @Test
    void aCallerCannotChooseItsOwnAddressByPrependingFakeForwardedEntries() {
        String real = newAddress();
        TestHttpClient first = new TestHttpClient(port);
        first.primeCsrfToken();
        int refused = 0;
        for (int i = 0; i < 16; i++) {
            // A different invented address on the left every time; the trusted proxy's entry on the right never changes.
            first.withHeader("X-Forwarded-For", "10.9." + i + ".1, " + real);
            ApiResult result = first.post("/auth/login", Map.of("email", TestData.uniqueEmail("spoof"), "password", "x"));
            if (result.status() == 429) {
                refused++;
            }
        }
        assertThat(refused).as("still counted as one address").isEqualTo(4);
    }

    @Test
    void everythingElseHasAGeneralCeilingPerAddress() {
        TestHttpClient client = clientAt(newAddress());
        int refused = 0;
        for (int i = 0; i < 50; i++) {
            if (client.get("/health").status() == 429) {
                refused++;
            }
        }
        assertThat(refused).as("40 a minute allowed, the rest refused").isGreaterThanOrEqualTo(9);
    }

    @Test
    void uploadsAreLimitedPerUserNotPerAddress() {
        String address = newAddress();
        TestHttpClient alice = clientAt(address);
        String aliceEmail = registerAt(alice);
        alice.post("/auth/login", Map.of("email", aliceEmail, "password", PASSWORD));

        // The files are rubbish on purpose: rejected as invalid, but each attempt still counts.
        byte[] junk = "not a pdf".getBytes();
        Map<String, String> form = Map.of("accountId", UUID.randomUUID().toString());
        assertThat(alice.postMultipart("/statements/upload", form, "file", "a.pdf", junk).status()).isNotEqualTo(429);
        assertThat(alice.postMultipart("/statements/upload", form, "file", "a.pdf", junk).status()).isNotEqualTo(429);
        ApiResult third = alice.postMultipart("/statements/upload", form, "file", "a.pdf", junk);
        assertThat(third.status()).isEqualTo(429);
        assertThat(third.errorCode()).isEqualTo("RATE_LIMITED");

        // Another person at the same address has their own allowance.
        TestHttpClient bob = clientAt(address);
        String bobEmail = registerAt(bob);
        bob.post("/auth/login", Map.of("email", bobEmail, "password", PASSWORD));
        assertThat(bob.postMultipart("/statements/upload", form, "file", "b.pdf", junk).status()).isNotEqualTo(429);
    }

    @Test
    void aRefusalCarriesTheCorsHeadersSoTheBrowserCanReadTheMessage() {
        TestHttpClient client = clientAt(newAddress()).withHeader("Origin", "https://app.example.com");
        ApiResult last = null;
        for (int i = 0; i < 14; i++) {
            last = client.post("/auth/login", Map.of("email", TestData.uniqueEmail("cors"), "password", "x"));
        }
        assertThat(last.status()).isEqualTo(429);
        assertThat(last.headers().firstValue("Access-Control-Allow-Origin")).contains("https://app.example.com");
        assertThat(last.headers().firstValue("Access-Control-Expose-Headers").orElse("")).contains("Retry-After");
    }
}
