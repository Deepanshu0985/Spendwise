package com.finance.bulk;

import com.fasterxml.jackson.databind.JsonNode;
import com.finance.support.ApiResult;
import com.finance.support.PdfFixtures;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Phase 16a: one category on many rows at once, safely: only the person's own rows, all or nothing, with an undo. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class BulkCategorizationIT {

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    private TestHttpClient client;
    private String accountId;
    private String food;
    private String travel;

    @BeforeEach
    void setUp() {
        client = signedIn("bulk");
        accountId = client.post("/accounts", Map.of("name", "Bank", "accountType", "BANK", "currency", "INR")).data().get("id").asText();
        JsonNode categories = client.get("/categories").data();
        food = categories.get(0).get("id").asText();
        travel = categories.get(1).get("id").asText();
    }

    private TestHttpClient signedIn(String label) {
        TestHttpClient c = new TestHttpClient(port);
        c.primeCsrfToken();
        String email = TestData.uniqueEmail(label);
        c.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Bulk Test"));
        c.post("/auth/login", Map.of("email", email, "password", PASSWORD));
        return c;
    }

    private String upload(TestHttpClient c, String account, String... rows) {
        byte[] pdf = PdfFixtures.statementPdf("HDFC BANK Statement of Account", List.of(rows));
        ApiResult result = c.postMultipart("/statements/upload", Map.of("accountId", account), "file", "s-" + UUID.randomUUID() + ".pdf", pdf);
        assertThat(result.status()).isEqualTo(200);
        return result.data().get("id").asText();
    }

    private String uploadThree() {
        return upload(client, accountId,
                "15/09/26 RAHUL TRADERS UPI111111 15/09/26 100.00 0.00 9900.00",
                "16/09/26 SHARMA STORE UPI222222 16/09/26 200.00 0.00 9700.00",
                "17/09/26 KUMAR CAFE UPI333333 17/09/26 300.00 0.00 9400.00");
    }

    private List<String> rowIds(String statementId) {
        List<String> ids = new ArrayList<>();
        client.get("/statements/" + statementId + "/transactions").data().forEach(r -> ids.add(r.get("id").asText()));
        return ids;
    }

    private JsonNode row(String statementId, String rowId) {
        for (JsonNode r : client.get("/statements/" + statementId + "/transactions").data()) {
            if (r.get("id").asText().equals(rowId)) {
                return r;
            }
        }
        throw new AssertionError("no row " + rowId);
    }

    private Map<String, Object> change(List<String> ids, String categoryId, boolean remember) {
        Map<String, Object> body = new HashMap<>();
        body.put("rowIds", ids);
        body.put("categoryId", categoryId);
        body.put("rememberPayee", remember);
        return body;
    }

    @Test
    void setsTheCategoryOnTheSelectedRowsOnlyAndMarksThemEdited() {
        String statementId = uploadThree();
        List<String> ids = rowIds(statementId);

        ApiResult result = client.post("/statements/" + statementId + "/transactions/bulk", change(ids.subList(0, 2), food, false));
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.data().get("changedCount").asInt()).isEqualTo(2);
        assertThat(result.data().get("rows")).hasSize(3);

        assertThat(row(statementId, ids.get(0)).get("suggestedCategoryId").asText()).isEqualTo(food);
        assertThat(row(statementId, ids.get(0)).get("reviewStatus").asText()).isEqualTo("EDITED");
        assertThat(row(statementId, ids.get(1)).get("suggestedCategoryId").asText()).isEqualTo(food);
        assertThat(row(statementId, ids.get(2)).get("suggestedCategoryId").isNull()).as("not selected, not touched").isTrue();
        assertThat(row(statementId, ids.get(2)).get("reviewStatus").asText()).isEqualTo("PENDING");
    }

    @Test
    void undoPutsTheRowsBackExactly() {
        String statementId = uploadThree();
        List<String> ids = rowIds(statementId);
        client.post("/statements/" + statementId + "/transactions/bulk", change(ids.subList(0, 1), travel, false));

        ApiResult second = client.post("/statements/" + statementId + "/transactions/bulk", change(ids, food, false));
        assertThat(second.status()).isEqualTo(200);
        assertThat(row(statementId, ids.get(0)).get("suggestedCategoryId").asText()).isEqualTo(food);

        ApiResult undone = client.post("/statements/" + statementId + "/transactions/bulk-restore", second.data().get("undo"));
        assertThat(undone.status()).isEqualTo(200);
        assertThat(row(statementId, ids.get(0)).get("suggestedCategoryId").asText()).as("back to the earlier choice").isEqualTo(travel);
        assertThat(row(statementId, ids.get(1)).get("suggestedCategoryId").isNull()).isTrue();
        assertThat(row(statementId, ids.get(1)).get("reviewStatus").asText()).isEqualTo("PENDING");
    }

    @Test
    void rememberingThePayeeFillsLaterStatementsAndUndoForgetsIt() {
        String statementId = uploadThree();
        String rahulsRow = null;
        for (JsonNode r : client.get("/statements/" + statementId + "/transactions").data()) {
            if (r.get("rawDescription").asText().contains("RAHUL")) {
                rahulsRow = r.get("id").asText();
            }
        }
        ApiResult result = client.post("/statements/" + statementId + "/transactions/bulk", change(List.of(rahulsRow), food, true));
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.data().get("undo").get("rules").get("createdRuleIds")).hasSize(1);

        String later = upload(client, accountId, "20/09/26 RAHUL TRADERS UPI444444 20/09/26 150.00 0.00 9000.00");
        assertThat(rowIdsAndCategory(later)).as("a later statement is filled in from the remembered payee").isEqualTo(food);

        client.post("/statements/" + statementId + "/transactions/bulk-restore", result.data().get("undo"));
        String afterUndo = upload(client, accountId, "21/09/26 RAHUL TRADERS UPI555555 21/09/26 175.00 0.00 8800.00");
        assertThat(rowIdsAndCategory(afterUndo)).as("undo forgot the payee").isNull();
    }

    private String rowIdsAndCategory(String statementId) {
        JsonNode first = client.get("/statements/" + statementId + "/transactions").data().get(0);
        return first.get("suggestedCategoryId").isNull() ? null : first.get("suggestedCategoryId").asText();
    }

    @Test
    void rememberingAlsoFillsTheOtherUntouchedRowsOfTheSamePayee() {
        String statementId = upload(client, accountId,
                "15/09/26 RAHUL TRADERS UPI111111 15/09/26 100.00 0.00 9900.00",
                "16/09/26 RAHUL TRADERS UPI222222 16/09/26 200.00 0.00 9700.00",
                "17/09/26 KUMAR CAFE UPI333333 17/09/26 300.00 0.00 9400.00");
        List<String> ids = rowIds(statementId);
        // find which row ids are Rahul's
        List<String> rahul = new ArrayList<>();
        for (JsonNode r : client.get("/statements/" + statementId + "/transactions").data()) {
            if (r.get("rawDescription").asText().contains("RAHUL")) {
                rahul.add(r.get("id").asText());
            }
        }
        assertThat(rahul).hasSize(2);

        ApiResult result = client.post("/statements/" + statementId + "/transactions/bulk", change(rahul.subList(0, 1), food, true));
        assertThat(result.status()).isEqualTo(200);
        assertThat(row(statementId, rahul.get(1)).get("suggestedCategoryId").asText()).as("the other Rahul row is filled in too").isEqualTo(food);
        String cafe = ids.stream().filter(id -> !rahul.contains(id)).findFirst().orElseThrow();
        assertThat(row(statementId, cafe).get("suggestedCategoryId").isNull()).isTrue();

        client.post("/statements/" + statementId + "/transactions/bulk-restore", result.data().get("undo"));
        assertThat(row(statementId, rahul.get(0)).get("suggestedCategoryId").isNull()).isTrue();
        assertThat(row(statementId, rahul.get(1)).get("suggestedCategoryId").isNull()).as("undo reaches the filled-in row too").isTrue();
    }

    @Test
    void aRowThatIsNotTheirsRejectsTheWholeBatchAndChangesNothing() {
        String statementId = uploadThree();
        List<String> ids = rowIds(statementId);

        TestHttpClient stranger = signedIn("stranger");
        String strangerAccount = stranger.post("/accounts", Map.of("name", "Bank", "accountType", "BANK", "currency", "INR")).data().get("id").asText();
        String strangerStatement = upload(stranger, strangerAccount, "15/09/26 SECRET PAYEE UPI999999 15/09/26 100.00 0.00 9900.00");
        String strangerRow = stranger.get("/statements/" + strangerStatement + "/transactions").data().get(0).get("id").asText();

        List<String> mixed = new ArrayList<>(ids.subList(0, 1));
        mixed.add(strangerRow);
        ApiResult result = client.post("/statements/" + statementId + "/transactions/bulk", change(mixed, food, false));
        assertThat(result.status()).isEqualTo(404);
        assertThat(row(statementId, ids.get(0)).get("suggestedCategoryId").isNull()).as("their own row was not changed either").isTrue();

        assertThat(client.post("/statements/" + strangerStatement + "/transactions/bulk", change(List.of(strangerRow), food, false)).status())
                .as("someone else's statement").isEqualTo(404);
        assertThat(stranger.get("/statements/" + strangerStatement + "/transactions").data().get(0).get("suggestedCategoryId").isNull()).isTrue();
    }

    @Test
    void aRowFromAnotherOfTheirStatementsIsRefused() {
        String one = uploadThree();
        String two = upload(client, accountId, "25/09/26 OTHER PAYEE UPI666666 25/09/26 50.00 0.00 9000.00");
        String foreign = rowIds(two).get(0);
        assertThat(client.post("/statements/" + one + "/transactions/bulk", change(List.of(foreign), food, false)).status()).isEqualTo(404);
        assertThat(row(two, foreign).get("suggestedCategoryId").isNull()).isTrue();
    }

    @Test
    void anImportedStatementCannotBeChanged() {
        String statementId = uploadThree();
        List<String> ids = rowIds(statementId);
        assertThat(client.post("/statements/" + statementId + "/confirm", Map.of()).status()).isEqualTo(200);
        ApiResult result = client.post("/statements/" + statementId + "/transactions/bulk", change(ids, food, false));
        assertThat(result.status()).isEqualTo(400);
        assertThat(result.errorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void aSkippedRowStaysSkipped() {
        String statementId = uploadThree();
        List<String> ids = rowIds(statementId);
        client.post("/statements/" + statementId + "/transactions/" + ids.get(0) + "/skip", Map.of());
        client.post("/statements/" + statementId + "/transactions/bulk", change(ids, food, false));
        assertThat(row(statementId, ids.get(0)).get("reviewStatus").asText()).isEqualTo("REJECTED");
        assertThat(row(statementId, ids.get(0)).get("suggestedCategoryId").asText()).isEqualTo(food);
    }

    @Test
    void badRequestsAreRefused() {
        String statementId = uploadThree();
        List<String> ids = rowIds(statementId);
        assertThat(client.post("/statements/" + statementId + "/transactions/bulk", Map.of("rowIds", ids)).status()).as("nothing to set").isEqualTo(400);
        assertThat(client.post("/statements/" + statementId + "/transactions/bulk", Map.of("rowIds", List.of(), "categoryId", food)).status()).isEqualTo(400);
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            tooMany.add(UUID.randomUUID().toString());
        }
        assertThat(client.post("/statements/" + statementId + "/transactions/bulk", Map.of("rowIds", tooMany, "categoryId", food)).status()).isEqualTo(400);

        TestHttpClient stranger = signedIn("cat-owner");
        // a category that exists but belongs to nobody visible to this person
        String othersCategory = stranger.post("/categories", Map.of("name", "Private", "categoryType", "EXPENSE")).data().get("id").asText();
        assertThat(client.post("/statements/" + statementId + "/transactions/bulk", change(ids, othersCategory, false)).status()).isEqualTo(404);
    }

    // ----- saved transactions -----

    private String addTransaction(TestHttpClient c, String account, String categoryId, int amount, List<Map<String, Object>> splits) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "accountId", account, "transactionDate", LocalDate.now().toString(), "amount", amount, "currency", "INR",
                "description", "test " + UUID.randomUUID(), "transactionType", "EXPENSE"));
        if (categoryId != null) {
            body.put("categoryId", categoryId);
        }
        if (splits != null) {
            body.put("splits", splits);
        }
        ApiResult created = c.post("/transactions", body);
        assertThat(created.status()).isEqualTo(200);
        return created.data().get("id").asText();
    }

    private JsonNode transaction(String id) {
        return client.get("/transactions/" + id).data();
    }

    @Test
    void savedTransactionsGetTheCategoryAndUndoRestoresThem() {
        String a = addTransaction(client, accountId, travel, 100, null);
        String b = addTransaction(client, accountId, null, 200, null);
        String c = addTransaction(client, accountId, travel, 300, null);

        ApiResult result = client.post("/transactions/bulk", Map.of("transactionIds", List.of(a, b), "categoryId", food));
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.data().get("changed")).hasSize(2);
        assertThat(transaction(a).get("categoryId").asText()).isEqualTo(food);
        assertThat(transaction(b).get("categoryId").asText()).isEqualTo(food);
        assertThat(transaction(c).get("categoryId").asText()).as("not selected").isEqualTo(travel);
        assertThat(transaction(a).get("amount").asDouble()).as("nothing else about it changed").isEqualTo(100.0);

        assertThat(client.post("/transactions/bulk-restore", result.data().get("undo")).status()).isEqualTo(200);
        assertThat(transaction(a).get("categoryId").asText()).isEqualTo(travel);
        assertThat(transaction(b).get("categoryId").isNull()).isTrue();
    }

    @Test
    void transactionsDividedAcrossCategoriesAreSkippedAndReported() {
        String split = addTransaction(client, accountId, null, 100,
                List.of(Map.of("categoryId", travel, "amount", 60), Map.of("categoryId", food, "amount", 40)));
        String plain = addTransaction(client, accountId, null, 50, null);

        ApiResult result = client.post("/transactions/bulk", Map.of("transactionIds", List.of(split, plain), "categoryId", food));
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.data().get("skippedWithSplits").asInt()).isEqualTo(1);
        assertThat(result.data().get("changed")).hasSize(1);
        assertThat(transaction(plain).get("categoryId").asText()).isEqualTo(food);
        assertThat(transaction(split).get("categoryId").isNull()).isTrue();
    }

    @Test
    void anotherPersonsOrADeletedTransactionRejectsTheWholeBatch() {
        String mine = addTransaction(client, accountId, travel, 100, null);
        String deleted = addTransaction(client, accountId, travel, 100, null);
        client.delete("/transactions/" + deleted);

        TestHttpClient stranger = signedIn("tx-stranger");
        String strangerAccount = stranger.post("/accounts", Map.of("name", "Bank", "accountType", "BANK", "currency", "INR")).data().get("id").asText();
        String theirs = addTransaction(stranger, strangerAccount, null, 999, null);

        assertThat(client.post("/transactions/bulk", Map.of("transactionIds", List.of(mine, theirs), "categoryId", food)).status()).isEqualTo(404);
        assertThat(client.post("/transactions/bulk", Map.of("transactionIds", List.of(mine, deleted), "categoryId", food)).status()).isEqualTo(404);
        assertThat(transaction(mine).get("categoryId").asText()).as("nothing was applied").isEqualTo(travel);
        assertThat(stranger.get("/transactions/" + theirs).data().get("categoryId").isNull()).isTrue();
    }
}
