package com.finance.analytics;

import com.finance.support.ApiResult;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End to end against real Postgres: automates analytics-specification.md's
 * worked example through the real HTTP API (register, accounts, categories,
 * merchants, transactions, transfer) rather than constructing domain objects
 * directly, so this also exercises RLS scoping on the raw JdbcTemplate queries
 * AnalyticsRepositoryImpl uses (see DECISIONS.md).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class AnalyticsFlowIT {

    private static final String PASSWORD = "correcthorse123";
    private static final String FROM = "2026-09-01";
    private static final String TO = "2026-09-30";

    @LocalServerPort
    private int port;

    private TestHttpClient client;
    private String cardAccountId;
    private String bankAccountId;
    private String foodCategoryId;
    private String groceriesCategoryId;

    @BeforeEach
    void setUp() {
        client = new TestHttpClient(port);
        client.primeCsrfToken();
        String email = TestData.uniqueEmail("analytics");
        client.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Analytics Test"));
        client.post("/auth/login", Map.of("email", email, "password", PASSWORD));

        cardAccountId = client.post("/accounts", Map.of("name", "Card", "accountType", "CREDIT_CARD", "currency", "INR"))
                .data().get("id").asText();
        bankAccountId = client.post("/accounts", Map.of("name", "Bank", "accountType", "BANK", "currency", "INR"))
                .data().get("id").asText();

        var categories = client.get("/categories").data();
        foodCategoryId = categories.get(0).get("id").asText();
        groceriesCategoryId = categories.get(1).get("id").asText();

        String swiggyMerchantId = client.post("/merchants", Map.of("canonicalName", "Swiggy")).data().get("id").asText();
        String groceriesMerchantId = client.post("/merchants", Map.of("canonicalName", "Groceries Store")).data().get("id").asText();

        // analytics-specification.md's Worked Example: one credit card and one bank account, September.
        createTransaction(cardAccountId, swiggyMerchantId, foodCategoryId, "2026-09-03", "500", "EXPENSE");
        createTransaction(cardAccountId, groceriesMerchantId, groceriesCategoryId, "2026-09-08", "2000", "EXPENSE");
        client.post("/transactions/transfer", Map.of(
                "fromAccountId", bankAccountId, "toAccountId", cardAccountId, "transactionDate", "2026-09-20",
                "amount", 2500, "currency", "INR", "kind", "CARD_PAYMENT"));
        createTransaction(cardAccountId, swiggyMerchantId, foodCategoryId, "2026-09-25", "500", "REFUND");
        createTransaction(bankAccountId, null, null, "2026-09-30", "80000", "INCOME");
    }

    @Test
    void monthlySummaryMatchesTheWorkedExample() {
        ApiResult result = client.get("/analytics/monthly?from=" + FROM + "&to=" + TO + "&currency=INR");
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.data().get("currency").asText()).isEqualTo("INR");
        assertThat(result.data().get("expenses").decimalValue()).isEqualByComparingTo("2000");
        assertThat(result.data().get("income").decimalValue()).isEqualByComparingTo("80000");
        assertThat(result.data().get("savings").decimalValue()).isEqualByComparingTo("78000");
        assertThat(result.data().get("savingsRate").decimalValue()).isEqualByComparingTo("97.5");
        assertThat(result.meta().get("excludedCurrencies")).isEmpty();
        assertThat(result.meta().get("excludedTransactionCount").asLong()).isZero();
    }

    @Test
    void categoryBreakdownSumsToExpensesAndNetsTheRefundAgainstItsOwnCategory() {
        ApiResult result = client.get("/analytics/categories?from=" + FROM + "&to=" + TO);
        assertThat(result.status()).isEqualTo(200);

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.math.BigDecimal groceriesAmount = null;
        java.math.BigDecimal foodAmount = null;
        for (var entry : result.data()) {
            total = total.add(entry.get("amount").decimalValue());
            String categoryId = entry.get("categoryId").isNull() ? null : entry.get("categoryId").asText();
            if (groceriesCategoryId.equals(categoryId)) {
                groceriesAmount = entry.get("amount").decimalValue();
            } else if (foodCategoryId.equals(categoryId)) {
                foodAmount = entry.get("amount").decimalValue();
            }
        }
        assertThat(total).isEqualByComparingTo("2000");
        assertThat(groceriesAmount).isEqualByComparingTo("2000");
        // Swiggy's 500 expense and its own 500 refund cancel out to exactly zero.
        assertThat(foodAmount).isEqualByComparingTo("0");
    }

    @Test
    void merchantBreakdownSumsToExpenses() {
        ApiResult result = client.get("/analytics/merchants?from=" + FROM + "&to=" + TO);
        assertThat(result.status()).isEqualTo(200);

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (var entry : result.data()) {
            total = total.add(entry.get("amount").decimalValue());
        }
        assertThat(total).isEqualByComparingTo("2000");
    }

    @Test
    void trendHasOnePointForSeptemberMatchingTheMonthlyTotal() {
        ApiResult result = client.get("/analytics/trends?from=" + FROM + "&to=" + TO);
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.data()).hasSize(1);
        var point = result.data().get(0);
        assertThat(point.get("month").asText()).isEqualTo("2026-09");
        assertThat(point.get("expenses").decimalValue()).isEqualByComparingTo("2000");
        assertThat(point.get("income").decimalValue()).isEqualByComparingTo("80000");
    }

    @Test
    void excludesTransactionsInOtherCurrenciesAndReportsThemRatherThanDroppingThemSilently() {
        String usdAccountId = client.post("/accounts", Map.of("name", "US Account", "accountType", "BANK", "currency", "USD"))
                .data().get("id").asText();
        createTransaction(usdAccountId, null, null, "2026-09-15", "10", "EXPENSE", "USD");

        ApiResult result = client.get("/analytics/monthly?from=" + FROM + "&to=" + TO + "&currency=INR");
        assertThat(result.status()).isEqualTo(200);
        // The INR figures are unaffected by the USD transaction.
        assertThat(result.data().get("expenses").decimalValue()).isEqualByComparingTo("2000");
        assertThat(result.meta().get("excludedCurrencies")).hasSize(1);
        assertThat(result.meta().get("excludedCurrencies").get(0).asText()).isEqualTo("USD");
        assertThat(result.meta().get("excludedTransactionCount").asLong()).isEqualTo(1);
    }

    @Test
    void rejectsAMissingPeriodAndAPeriodOverThirtySixMonths() {
        ApiResult missing = client.get("/analytics/monthly?currency=INR");
        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.errorCode()).isEqualTo("VALIDATION_ERROR");

        ApiResult tooLong = client.get("/analytics/monthly?from=2020-01-01&to=2023-06-01&currency=INR");
        assertThat(tooLong.status()).isEqualTo(400);
        assertThat(tooLong.errorCode()).isEqualTo("VALIDATION_ERROR");

        ApiResult backwards = client.get("/analytics/monthly?from=2026-09-30&to=2026-09-01&currency=INR");
        assertThat(backwards.status()).isEqualTo(400);
    }

    private void createTransaction(String accountId, String merchantId, String categoryId, String date, String amount, String type) {
        createTransaction(accountId, merchantId, categoryId, date, amount, type, "INR");
    }

    private void createTransaction(String accountId, String merchantId, String categoryId, String date, String amount, String type, String currency) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("accountId", accountId);
        body.put("transactionDate", date);
        body.put("amount", new java.math.BigDecimal(amount));
        body.put("currency", currency);
        body.put("transactionType", type);
        if (merchantId != null) {
            body.put("merchantId", merchantId);
        }
        if (categoryId != null) {
            body.put("categoryId", categoryId);
        }
        ApiResult result = client.post("/transactions", body);
        assertThat(result.status()).as("failed to create fixture transaction: %s", result.body()).isEqualTo(200);
    }
}
