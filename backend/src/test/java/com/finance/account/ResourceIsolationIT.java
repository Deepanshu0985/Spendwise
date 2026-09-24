package com.finance.account;

import com.finance.support.ApiResult;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The cross-user isolation matrix from testing-strategy.md, for Phase 2's
 * resources. Automates what was originally hand-verified with two curl
 * sessions (Alice/Bob) - see DECISIONS.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class ResourceIsolationIT {

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    @Test
    void secondUserNeverSeesTheFirstUsersAccountsCategoriesOrMerchants() {
        TestHttpClient userA = registerAndLogin("isolation-a");
        TestHttpClient userB = registerAndLogin("isolation-b");

        ApiResult account = userA.post(
                "/accounts", Map.of("name", "A's Account", "accountType", "BANK", "currency", "INR"));
        assertThat(account.status()).isEqualTo(200);
        String accountId = account.data().get("id").asText();

        ApiResult category = userA.post("/categories", Map.of("name", "A's Category", "categoryType", "EXPENSE"));
        assertThat(category.status()).isEqualTo(200);

        ApiResult merchant = userA.post("/merchants", Map.of("canonicalName", "A's Merchant"));
        assertThat(merchant.status()).isEqualTo(200);

        assertThat(namesOf(userB.get("/accounts"))).isEmpty();
        assertThat(namesOf(userB.get("/merchants"))).isEmpty();
        assertThat(namesOf(userB.get("/categories"))).doesNotContain("A's Category");

        // System categories stay visible to everyone (18 seeded rows, ADR-015);
        // B has no custom categories of their own, so this is exactly 18.
        assertThat(userB.get("/categories").data()).hasSize(18);

        // Not "does B's list omit it" but "can B reach it directly at all" -
        // ownership failures return 404, never 403 (error-contract.md), so
        // identifiers are not confirmed to exist across tenants.
        ApiResult crossTenantGet = userB.get("/accounts/" + accountId);
        assertThat(crossTenantGet.status()).isEqualTo(404);

        ApiResult crossTenantUpdate = userB.put("/accounts/" + accountId, Map.of("name", "Hijacked"));
        assertThat(crossTenantUpdate.status()).isEqualTo(404);

        ApiResult crossTenantDelete = userB.delete("/accounts/" + accountId);
        assertThat(crossTenantDelete.status()).isEqualTo(404);
    }

    private java.util.List<String> namesOf(ApiResult listResult) {
        assertThat(listResult.status()).isEqualTo(200);
        java.util.List<String> names = new java.util.ArrayList<>();
        listResult.data().forEach(node -> names.add(node.has("name") ? node.get("name").asText() : node.get("canonicalName").asText()));
        return names;
    }

    private TestHttpClient registerAndLogin(String label) {
        TestHttpClient client = new TestHttpClient(port);
        client.primeCsrfToken();
        String email = TestData.uniqueEmail(label);
        client.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", label));
        client.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        return client;
    }
}
