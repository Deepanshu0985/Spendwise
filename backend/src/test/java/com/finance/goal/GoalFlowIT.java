package com.finance.goal;

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

/** Savings goals: create/list/update/delete, progress, status, validation, and tenant isolation. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class GoalFlowIT {

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    private TestHttpClient client;

    @BeforeEach
    void setUp() {
        client = signedInClient("goal-flow");
    }

    private TestHttpClient signedInClient(String label) {
        TestHttpClient c = new TestHttpClient(port);
        c.primeCsrfToken();
        String email = TestData.uniqueEmail(label);
        c.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Goal Test"));
        c.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        return c;
    }

    @Test
    void aGoalShowsProgressAndTheMonthlyAmountNeeded() {
        LocalDate target = LocalDate.now().plusMonths(6);
        ApiResult created = client.post("/goals", Map.of(
                "name", "Laptop", "targetAmount", 60000, "currentAmount", 30000, "targetDate", target.toString(), "currency", "INR"));

        assertThat(created.status()).isEqualTo(200);
        var goal = created.data();
        assertThat(goal.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(goal.get("remaining").asDouble()).isEqualTo(30000.0);
        assertThat(goal.get("percentComplete").asDouble()).isEqualTo(50.0);
        assertThat(goal.get("requiredPerMonth").asDouble()).isEqualTo(5000.0);
        assertThat(goal.get("overdue").asBoolean()).isFalse();
        assertThat(client.get("/goals").data()).hasSize(1);
    }

    @Test
    void addingMoneyUntilTheTargetIsReachedAchievesTheGoal() {
        String id = client.post("/goals", Map.of("name", "Trip", "targetAmount", 10000, "currency", "INR")).data().get("id").asText();

        ApiResult partial = client.put("/goals/" + id, Map.of("name", "Trip", "targetAmount", 10000, "currentAmount", 4000));
        assertThat(partial.data().get("status").asText()).isEqualTo("ACTIVE");
        assertThat(partial.data().get("requiredPerMonth").isNull()).isTrue();

        ApiResult done = client.put("/goals/" + id, Map.of("name", "Trip", "targetAmount", 10000, "currentAmount", 10000));
        assertThat(done.data().get("status").asText()).isEqualTo("ACHIEVED");
        assertThat(done.data().get("percentComplete").asDouble()).isEqualTo(100.0);

        ApiResult raised = client.put("/goals/" + id, Map.of("name", "Trip", "targetAmount", 15000, "currentAmount", 10000));
        assertThat(raised.data().get("status").asText()).isEqualTo("ACTIVE");
    }

    @Test
    void aGoalPastItsDateIsOverdueAndCanBeDeleted() {
        String id = client.post("/goals", Map.of(
                "name", "Old goal", "targetAmount", 5000, "currentAmount", 1000,
                "targetDate", LocalDate.now().minusDays(3).toString(), "currency", "INR")).data().get("id").asText();

        assertThat(client.get("/goals").data().get(0).get("overdue").asBoolean()).isTrue();
        assertThat(client.delete("/goals/" + id).status()).isEqualTo(204);
        assertThat(client.get("/goals").data()).isEmpty();
        assertThat(client.delete("/goals/" + id).status()).isEqualTo(404);
    }

    @Test
    void invalidGoalsAreRejected() {
        assertThat(client.post("/goals", Map.of("name", "Zero", "targetAmount", 0, "currency", "INR")).status()).isBetween(400, 499);
        assertThat(client.post("/goals", Map.of("name", "Negative", "targetAmount", 100, "currentAmount", -1, "currency", "INR")).status())
                .isBetween(400, 499);
        assertThat(client.post("/goals", Map.of("name", "", "targetAmount", 100, "currency", "INR")).status()).isBetween(400, 499);
        assertThat(client.post("/goals", Map.of("name", "No currency", "targetAmount", 100)).status()).isBetween(400, 499);
    }

    @Test
    void anotherUserCannotSeeChangeOrDeleteMyGoal() {
        String id = client.post("/goals", Map.of("name", "Mine", "targetAmount", 1000, "currency", "INR")).data().get("id").asText();

        TestHttpClient stranger = signedInClient("goal-stranger");
        assertThat(stranger.get("/goals").data()).isEmpty();
        assertThat(stranger.put("/goals/" + id, Map.of("name", "Taken", "targetAmount", 1)).status()).isEqualTo(404);
        assertThat(stranger.delete("/goals/" + id).status()).isEqualTo(404);
        assertThat(client.get("/goals").data()).hasSize(1);
    }
}
