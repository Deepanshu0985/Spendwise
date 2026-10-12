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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The website forwards API calls to the backend, so there are two proxies in front of it: the website (which writes the
 * visitor's address into X-Forwarded-For) and the host's own (which appends the website's address). Two trusted hops.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestPropertySource(properties = {
        "ratelimit.enabled=true",
        "ratelimit.trusted-proxy-hops=2",
        "ratelimit.auth-per-five-minutes=4",
        "ratelimit.peer-multiplier=3"
})
class RateLimitBehindWebsiteProxyIT {

    @LocalServerPort
    private int port;

    private static String address() {
        return "198.51." + ((int) (Math.random() * 200) + 20) + "." + ((int) (Math.random() * 250) + 1);
    }

    private ApiResult tryLogin(TestHttpClient client) {
        return client.post("/auth/login", Map.of("email", TestData.uniqueEmail("proxy"), "password", "x"));
    }

    private TestHttpClient clientWithChain(String forwardedFor) {
        TestHttpClient client = new TestHttpClient(port).withHeader("X-Forwarded-For", forwardedFor);
        client.primeCsrfToken();
        return client;
    }

    @Test
    void visitorsBehindTheSameWebsiteAddressAreLimitedSeparately() {
        String website = address();
        TestHttpClient visitorA = clientWithChain(address() + ", " + website);
        TestHttpClient visitorB = clientWithChain(address() + ", " + website);

        int refusedA = 0;
        for (int i = 0; i < 6; i++) {
            if (tryLogin(visitorA).status() == 429) {
                refusedA++;
            }
        }
        assertThat(refusedA).as("visitor A is stopped after their own four").isEqualTo(2);
        assertThat(tryLogin(visitorB).status()).as("visitor B, same website address, is unaffected").isEqualTo(401);
    }

    @Test
    void someoneWhoSkipsTheWebsiteAndForgesTheClaimedAddressStillHitsAnOuterLimit() {
        String attacker = address();
        TestHttpClient direct = clientWithChain("1.1.1.1, " + attacker);
        int refused = 0;
        for (int i = 0; i < 16; i++) {
            direct.withHeader("X-Forwarded-For", "10.20." + i + ".1, " + attacker); // a new invented client every time
            if (tryLogin(direct).status() == 429) {
                refused++;
            }
        }
        assertThat(refused).as("12 allowed (4 x 3) from the address that really connected, the rest refused").isEqualTo(4);
    }
}
