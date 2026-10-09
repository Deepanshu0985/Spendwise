package com.finance.recurring;

import com.finance.support.ApiResult;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Detect -> list -> confirm/rename -> dismiss, and that one user can never touch another's patterns. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class RecurringFlowIT {

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    private TestHttpClient client;
    private String accountId;

    @BeforeEach
    void setUp() {
        client = signedInClient("recurring-flow");
        accountId = client.post("/accounts", Map.of("name", "Bank", "accountType", "BANK", "currency", "INR")).data().get("id").asText();
    }

    private TestHttpClient signedInClient(String label) {
        TestHttpClient c = new TestHttpClient(port);
        c.primeCsrfToken();
        String email = TestData.uniqueEmail(label);
        c.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Recurring Test"));
        c.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        return c;
    }

    private void addExpense(String description, LocalDate date, int amount) {
        ApiResult created = client.post("/transactions", Map.of(
                "accountId", accountId, "transactionDate", date.toString(), "amount", amount, "currency", "INR",
                "description", description, "transactionType", "EXPENSE"));
        assertThat(created.status()).isEqualTo(200);
    }

    /** Four monthly payments, the latest ten days ago, so the pattern is still running. */
    private void addMonthlySubscription(String description, int amount) {
        LocalDate latest = LocalDate.now().minusDays(10);
        for (int i = 0; i < 4; i++) {
            addExpense(description, latest.minusMonths(i), amount);
        }
    }

    private com.fasterxml.jackson.databind.JsonNode find(com.fasterxml.jackson.databind.JsonNode list, String name) {
        for (var item : list) {
            if (item.get("name").asText().equals(name)) {
                return item;
            }
        }
        return null;
    }

    @Test
    void detectFindsARepeatingPaymentAndIgnoresOneOffs() {
        addMonthlySubscription("Netflix", 649);
        addExpense("Sofa shop", LocalDate.now().minusDays(20), 18000);

        ApiResult detected = client.post("/recurring-expenses/detect", Map.of());
        assertThat(detected.status()).isEqualTo(200);
        assertThat(detected.data()).hasSize(1);
        var netflix = find(detected.data(), "Netflix");
        assertThat(netflix).isNotNull();
        assertThat(netflix.get("frequency").asText()).isEqualTo("MONTHLY");
        assertThat(netflix.get("averageAmount").asDouble()).isEqualTo(649.0);
        assertThat(netflix.get("yearlyEstimate").asDouble()).isEqualTo(7788.0);
        assertThat(netflix.get("monthlyEstimate").asDouble()).isEqualTo(649.0);
        assertThat(netflix.get("isActive").asBoolean()).isTrue();
        assertThat(netflix.get("confirmed").asBoolean()).isFalse();
        assertThat(netflix.get("nextExpectedDate").asText()).isEqualTo(LocalDate.now().minusDays(10).plusMonths(1).toString());

        assertThat(client.get("/recurring-expenses").data()).hasSize(1);
        // detection is repeatable and does not duplicate
        assertThat(client.post("/recurring-expenses/detect", Map.of()).data()).hasSize(1);
    }

    @Test
    void aUsersNameAndConfirmChoicesSurviveAReDetection() {
        addMonthlySubscription("Netflix", 649);
        String id = client.post("/recurring-expenses/detect", Map.of()).data().get(0).get("id").asText();

        ApiResult updated = client.put("/recurring-expenses/" + id, Map.of("name", "Netflix family plan", "confirmed", true));
        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.data().get("name").asText()).isEqualTo("Netflix family plan");

        ApiResult again = client.post("/recurring-expenses/detect", Map.of());
        assertThat(again.data()).hasSize(1);
        assertThat(again.data().get(0).get("name").asText()).isEqualTo("Netflix family plan");
        assertThat(again.data().get(0).get("confirmed").asBoolean()).isTrue();
    }

    @Test
    void aDismissedPatternStaysGoneThroughLaterDetections() {
        addMonthlySubscription("Netflix", 649);
        String id = client.post("/recurring-expenses/detect", Map.of()).data().get(0).get("id").asText();

        assertThat(client.post("/recurring-expenses/" + id + "/dismiss", Map.of()).status()).isEqualTo(204);
        assertThat(client.post("/recurring-expenses/" + id + "/dismiss", Map.of()).status()).isEqualTo(204);
        assertThat(client.get("/recurring-expenses").data()).isEmpty();
        assertThat(client.post("/recurring-expenses/detect", Map.of()).data()).isEmpty();
    }

    @Test
    void aPatternThatHasStoppedIsKeptButLeftOutOfTheActiveList() {
        LocalDate latest = LocalDate.now().minusMonths(6);
        for (int i = 0; i < 4; i++) {
            addExpense("Old OTT plan", latest.minusMonths(i), 299);
        }

        ApiResult detected = client.post("/recurring-expenses/detect", Map.of());
        assertThat(detected.data()).hasSize(1);
        assertThat(detected.data().get(0).get("isActive").asBoolean()).isFalse();
        assertThat(client.get("/recurring-expenses?activeOnly=true").data()).isEmpty();
        assertThat(client.get("/recurring-expenses").data()).hasSize(1);
    }

    @Test
    void anotherUserCannotSeeChangeOrDismissMyPatterns() {
        addMonthlySubscription("Netflix", 649);
        String id = client.post("/recurring-expenses/detect", Map.of()).data().get(0).get("id").asText();

        TestHttpClient stranger = signedInClient("recurring-stranger");
        assertThat(stranger.get("/recurring-expenses").data()).isEmpty();
        assertThat(stranger.post("/recurring-expenses/detect", Map.of()).data()).isEmpty();
        assertThat(stranger.put("/recurring-expenses/" + id, Map.of("name", "mine now")).status()).isEqualTo(404);
        assertThat(stranger.post("/recurring-expenses/" + id + "/dismiss", Map.of()).status()).isEqualTo(404);
        assertThat(client.get("/recurring-expenses").data()).hasSize(1);
    }

    // Smoke test only: the "it" profile pools a single database connection, so these calls end up running one after
    // another and this passes with or without the per-user lock. The lock itself was checked against the real
    // overlapping requests the page produces (see DECISIONS.md).
    @Test
    void overlappingDetectionsDoNotCreateTheSamePatternTwiceOrFail() throws Exception {
        addMonthlySubscription("Netflix", 649);

        var pool = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            java.util.List<java.util.concurrent.Future<Integer>> results = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                results.add(pool.submit(() -> client.post("/recurring-expenses/detect", Map.of()).status()));
            }
            for (var result : results) {
                assertThat(result.get()).isEqualTo(200);
            }
        } finally {
            pool.shutdown();
        }
        assertThat(client.get("/recurring-expenses").data()).hasSize(1);
    }

    @Test
    void anUnknownCategoryOnUpdateIsRejected() {
        addMonthlySubscription("Netflix", 649);
        String id = client.post("/recurring-expenses/detect", Map.of()).data().get(0).get("id").asText();

        ApiResult result = client.put("/recurring-expenses/" + id, Map.of("categoryId", java.util.UUID.randomUUID().toString()));
        assertThat(result.status()).isEqualTo(404);
    }
}
