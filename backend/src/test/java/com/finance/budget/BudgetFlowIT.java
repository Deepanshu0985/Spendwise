package com.finance.budget;

import com.finance.support.ApiResult;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Budgets: create/list/get/update/delete, progress from real transactions, validation, and tenant isolation. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class BudgetFlowIT {

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    private TestHttpClient client;
    private String accountId;
    private String categoryA;
    private String categoryB;

    @BeforeEach
    void setUp() {
        client = signedInClient("budget-flow");
        accountId = client.post("/accounts", Map.of("name", "Bank", "accountType", "BANK", "currency", "INR")).data().get("id").asText();
        var categories = client.get("/categories").data();
        categoryA = categories.get(0).get("id").asText();
        categoryB = categories.get(1).get("id").asText();
    }

    private TestHttpClient signedInClient(String label) {
        TestHttpClient c = new TestHttpClient(port);
        c.primeCsrfToken();
        String email = TestData.uniqueEmail(label);
        c.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Budget Test"));
        c.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        return c;
    }

    private void spend(String categoryId, LocalDate date, int amount, String type) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "accountId", accountId, "transactionDate", date.toString(), "amount", amount, "currency", "INR",
                "description", "test", "transactionType", type));
        if (categoryId != null) {
            body.put("categoryId", categoryId);
        }
        assertThat(client.post("/transactions", body).status()).isEqualTo(200);
    }

    private Map<String, Object> monthlyBudget(int total, Object... categoryLimitPairs) {
        List<Map<String, Object>> limits = new java.util.ArrayList<>();
        for (int i = 0; i < categoryLimitPairs.length; i += 2) {
            limits.add(Map.of("categoryId", categoryLimitPairs[i], "limitAmount", categoryLimitPairs[i + 1]));
        }
        return Map.of("name", "Monthly", "periodType", "MONTHLY", "totalLimit", total, "currency", "INR", "categoryLimits", limits);
    }

    private com.fasterxml.jackson.databind.JsonNode categoryLine(com.fasterxml.jackson.databind.JsonNode budget, String categoryId) {
        for (var line : budget.get("progress").get("categories")) {
            if (line.get("categoryId").asText().equals(categoryId)) {
                return line;
            }
        }
        throw new AssertionError("no progress line for category " + categoryId);
    }

    @Test
    void progressCountsOnlyThisMonthsExpensesAndMatchesTheLimits() {
        LocalDate today = LocalDate.now();
        spend(categoryA, today, 1500, "EXPENSE");
        spend(categoryA, today, 300, "EXPENSE");
        spend(categoryB, today, 500, "EXPENSE");
        spend(null, today, 200, "EXPENSE");
        spend(categoryA, today.withDayOfMonth(1).minusDays(1), 9999, "EXPENSE"); // last month: outside the window
        spend(categoryA, today, 7000, "INCOME"); // income is never spending

        ApiResult created = client.post("/budgets", monthlyBudget(10000, categoryA, 2000));
        assertThat(created.status()).isEqualTo(200);
        var budget = created.data();
        assertThat(budget.get("progress").get("totalSpent").asDouble()).isEqualTo(2500.0);
        assertThat(budget.get("progress").get("remaining").asDouble()).isEqualTo(7500.0);
        assertThat(budget.get("progress").get("percentUsed").asDouble()).isEqualTo(25.0);
        assertThat(budget.get("progress").get("status").asText()).isEqualTo("ON_TRACK");
        assertThat(budget.get("progress").get("windowStart").asText()).isEqualTo(today.withDayOfMonth(1).toString());

        var line = categoryLine(budget, categoryA);
        assertThat(line.get("spent").asDouble()).isEqualTo(1800.0);
        assertThat(line.get("percentUsed").asDouble()).isEqualTo(90.0);
        assertThat(line.get("status").asText()).isEqualTo("CLOSE_TO_LIMIT");

        // the same figures through list and get
        assertThat(client.get("/budgets").data()).hasSize(1);
        assertThat(client.get("/budgets/" + budget.get("id").asText()).data().get("progress").get("totalSpent").asDouble()).isEqualTo(2500.0);
    }

    @Test
    void aCustomBudgetMeasuresItsOwnDateRange() {
        LocalDate start = LocalDate.now().minusDays(40);
        spend(categoryA, start.plusDays(2), 800, "EXPENSE");
        spend(categoryA, LocalDate.now(), 5000, "EXPENSE"); // after the range

        ApiResult created = client.post("/budgets", Map.of(
                "name", "Spring", "periodType", "CUSTOM", "startDate", start.toString(), "endDate", start.plusDays(10).toString(),
                "totalLimit", 1000, "currency", "INR"));

        assertThat(created.status()).isEqualTo(200);
        assertThat(created.data().get("progress").get("totalSpent").asDouble()).isEqualTo(800.0);
        assertThat(created.data().get("progress").get("windowEnd").asText()).isEqualTo(start.plusDays(10).toString());
    }

    @Test
    void updateReplacesTheLimitsAndDeleteRemovesTheBudget() {
        String id = client.post("/budgets", monthlyBudget(10000, categoryA, 2000, categoryB, 3000)).data().get("id").asText();

        ApiResult updated = client.put("/budgets/" + id, Map.of(
                "name", "Renamed", "periodType", "MONTHLY", "totalLimit", 8000,
                "categoryLimits", List.of(Map.of("categoryId", categoryB, "limitAmount", 1000))));

        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.data().get("name").asText()).isEqualTo("Renamed");
        assertThat(updated.data().get("totalLimit").asDouble()).isEqualTo(8000.0);
        assertThat(updated.data().get("progress").get("categories")).hasSize(1);
        assertThat(updated.data().get("currency").asText()).isEqualTo("INR");

        assertThat(client.delete("/budgets/" + id).status()).isEqualTo(204);
        assertThat(client.get("/budgets/" + id).status()).isEqualTo(404);
        assertThat(client.get("/budgets").data()).isEmpty();
    }

    @Test
    void invalidBudgetsAreRejected() {
        Map<String, Object> duplicateCategory = monthlyBudget(5000, categoryA, 1000, categoryA, 2000);
        assertThat(client.post("/budgets", duplicateCategory).status()).isBetween(400, 499);

        assertThat(client.post("/budgets", monthlyBudget(5000, UUID.randomUUID().toString(), 1000)).status()).isEqualTo(404);
        assertThat(client.post("/budgets", monthlyBudget(0)).status()).isBetween(400, 499);
        assertThat(client.post("/budgets", monthlyBudget(5000, categoryA, -5)).status()).isBetween(400, 499);

        assertThat(client.post("/budgets", Map.of(
                "name", "Backwards", "periodType", "CUSTOM", "startDate", "2026-05-10", "endDate", "2026-05-01",
                "totalLimit", 1000, "currency", "INR")).status()).isBetween(400, 499);
        assertThat(client.post("/budgets", Map.of(
                "name", "No dates", "periodType", "CUSTOM", "totalLimit", 1000, "currency", "INR")).status()).isBetween(400, 499);
        assertThat(client.post("/budgets", Map.of("name", "No currency", "periodType", "MONTHLY", "totalLimit", 1000)).status())
                .isBetween(400, 499);
    }

    @Test
    void anotherUserCannotSeeChangeOrDeleteMyBudget() {
        String id = client.post("/budgets", monthlyBudget(10000, categoryA, 2000)).data().get("id").asText();

        TestHttpClient stranger = signedInClient("budget-stranger");
        assertThat(stranger.get("/budgets").data()).isEmpty();
        assertThat(stranger.get("/budgets/" + id).status()).isEqualTo(404);
        assertThat(stranger.put("/budgets/" + id, monthlyBudget(1)).status()).isEqualTo(404);
        assertThat(stranger.delete("/budgets/" + id).status()).isEqualTo(404);
        assertThat(client.get("/budgets/" + id).status()).isEqualTo(200);
    }
}
