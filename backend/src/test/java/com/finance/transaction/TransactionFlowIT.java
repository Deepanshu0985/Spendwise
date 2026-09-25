package com.finance.transaction;

import com.finance.support.ApiResult;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Automates the curl-based verification this flow was hand-checked with (see DECISIONS.md, Phase 3). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class TransactionFlowIT {

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    private TestHttpClient client;
    private String accountId;
    private String cardAccountId;
    private String categoryId;

    @BeforeEach
    void setUp() {
        client = new TestHttpClient(port);
        client.primeCsrfToken();
        String email = TestData.uniqueEmail("tx-flow");
        client.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Tx Flow Test"));
        client.post("/auth/login", Map.of("email", email, "password", PASSWORD));

        accountId = client.post("/accounts", Map.of("name", "Bank", "accountType", "BANK", "currency", "INR"))
                .data().get("id").asText();
        cardAccountId = client.post("/accounts", Map.of("name", "Card", "accountType", "CREDIT_CARD", "currency", "INR"))
                .data().get("id").asText();
        categoryId = client.get("/categories").data().get(0).get("id").asText();
    }

    @Test
    void createsListsUpdatesAndSoftDeletesATransaction() {
        ApiResult create = client.post(
                "/transactions",
                Map.of(
                        "accountId", accountId,
                        "categoryId", categoryId,
                        "transactionDate", "2026-09-18",
                        "amount", 500,
                        "currency", "INR",
                        "description", "Lunch",
                        "transactionType", "EXPENSE"));
        assertThat(create.status()).isEqualTo(200);
        String txId = create.data().get("id").asText();
        assertThat(create.data().get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(create.data().get("source").asText()).isEqualTo("MANUAL");

        ApiResult list = client.get("/transactions");
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.data()).hasSize(1);

        ApiResult update = client.put(
                "/transactions/" + txId,
                Map.of("accountId", accountId, "transactionDate", "2026-09-18", "amount", 600, "description", "Lunch and coffee"));
        assertThat(update.status()).isEqualTo(200);
        assertThat(update.data().get("amount").asDouble()).isEqualTo(600.0);

        ApiResult delete = client.delete("/transactions/" + txId);
        assertThat(delete.status()).isEqualTo(204);

        ApiResult listAfterDelete = client.get("/transactions");
        assertThat(listAfterDelete.data()).isEmpty();

        ApiResult getDirect = client.get("/transactions/" + txId);
        assertThat(getDirect.status()).isEqualTo(200);
        assertThat(getDirect.data().get("status").asText()).isEqualTo("DELETED");
    }

    @Test
    void rejectsTransferTypesWrongCurrencyZeroAmountAndUnbalancedSplits() {
        ApiResult transferType = client.post(
                "/transactions",
                Map.of("accountId", accountId, "transactionDate", "2026-09-18", "amount", 500, "currency", "INR", "transactionType", "TRANSFER_OUT"));
        assertThat(transferType.status()).isEqualTo(400);
        assertThat(transferType.errorCode()).isEqualTo("VALIDATION_ERROR");

        ApiResult wrongCurrency = client.post(
                "/transactions",
                Map.of("accountId", accountId, "transactionDate", "2026-09-18", "amount", 500, "currency", "USD", "transactionType", "EXPENSE"));
        assertThat(wrongCurrency.status()).isEqualTo(400);

        ApiResult zeroAmount = client.post(
                "/transactions",
                Map.of("accountId", accountId, "transactionDate", "2026-09-18", "amount", 0, "currency", "INR", "transactionType", "EXPENSE"));
        assertThat(zeroAmount.status()).isEqualTo(400);

        ApiResult unbalancedSplits = client.post(
                "/transactions",
                Map.of(
                        "accountId", accountId,
                        "transactionDate", "2026-09-18",
                        "amount", 1000,
                        "currency", "INR",
                        "transactionType", "EXPENSE",
                        "splits", List.of(Map.of("categoryId", categoryId, "amount", 600))));
        assertThat(unbalancedSplits.status()).isEqualTo(400);
    }

    @Test
    void balancedSplitsAreAccepted() {
        ApiResult create = client.post(
                "/transactions",
                Map.of(
                        "accountId", accountId,
                        "transactionDate", "2026-09-18",
                        "amount", 1000,
                        "currency", "INR",
                        "transactionType", "EXPENSE",
                        "splits", List.of(
                                Map.of("categoryId", categoryId, "amount", 600),
                                Map.of("amount", 400))));
        assertThat(create.status()).isEqualTo(200);
        assertThat(create.data().get("splits")).hasSize(2);
    }

    @Test
    void repeatedIdempotencyKeyReturnsTheOriginalResponseWithoutDuplicating() {
        Map<String, Object> body = Map.of(
                "accountId", accountId, "transactionDate", "2026-09-21", "amount", 50, "currency", "INR",
                "transactionType", "EXPENSE", "description", "Coffee");

        client.primeCsrfToken();
        ApiResult first = postWithIdempotencyKey(body, "idem-key-1");
        ApiResult second = postWithIdempotencyKey(body, "idem-key-1");

        assertThat(first.status()).isEqualTo(200);
        assertThat(second.status()).isEqualTo(200);
        assertThat(second.data().get("id").asText()).isEqualTo(first.data().get("id").asText());

        ApiResult list = client.get("/transactions");
        assertThat(list.data().size()).isEqualTo(1);
    }

    @Test
    void cardPaymentTransferCreatesAnAtomicPairAndRejectsTheWrongDirection() {
        ApiResult transfer = client.post(
                "/transactions/transfer",
                Map.of(
                        "fromAccountId", accountId,
                        "toAccountId", cardAccountId,
                        "transactionDate", "2026-09-20",
                        "amount", 2500,
                        "currency", "INR",
                        "kind", "CARD_PAYMENT"));
        assertThat(transfer.status()).isEqualTo(200);
        String outType = transfer.data().get("outTransaction").get("transactionType").asText();
        String inType = transfer.data().get("inTransaction").get("transactionType").asText();
        String outGroup = transfer.data().get("outTransaction").get("transferGroupId").asText();
        String inGroup = transfer.data().get("inTransaction").get("transferGroupId").asText();
        assertThat(outType).isEqualTo("CARD_PAYMENT_OUT");
        assertThat(inType).isEqualTo("CARD_PAYMENT_IN");
        assertThat(outGroup).isEqualTo(inGroup);

        ApiResult wrongDirection = client.post(
                "/transactions/transfer",
                Map.of(
                        "fromAccountId", cardAccountId,
                        "toAccountId", accountId,
                        "transactionDate", "2026-09-20",
                        "amount", 100,
                        "currency", "INR",
                        "kind", "CARD_PAYMENT"));
        assertThat(wrongDirection.status()).isEqualTo(400);
    }

    private ApiResult postWithIdempotencyKey(Map<String, Object> body, String key) {
        return client.postWithHeader("/transactions", body, "Idempotency-Key", key);
    }
}
