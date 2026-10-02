package com.finance.statement;

import com.finance.support.ApiResult;
import com.finance.support.PdfFixtures;
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

/** Automates the upload -> review -> confirm flow (api-specification.md's Statements endpoints, pdf-processing.md's state machine). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class StatementFlowIT {

    private static final String PASSWORD = "correcthorse123";

    @LocalServerPort
    private int port;

    private TestHttpClient client;
    private String accountId;

    @BeforeEach
    void setUp() {
        client = new TestHttpClient(port);
        client.primeCsrfToken();
        String email = TestData.uniqueEmail("stmt-flow");
        client.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Statement Flow Test"));
        client.post("/auth/login", Map.of("email", email, "password", PASSWORD));

        accountId = client.post("/accounts", Map.of("name", "Bank", "accountType", "BANK", "currency", "INR"))
                .data().get("id").asText();
    }

    @Test
    void uploadReviewConfirmPromotesStagedRowsIntoRealTransactions() {
        byte[] pdf = PdfFixtures.statementPdf(
                "HDFC BANK Statement of Account",
                List.of(
                        "15/09/26 SWIGGY BANGALORE UPI123456 15/09/26 500.00 0.00 4500.00",
                        "18/09/26 SALARY CREDIT ACME CORP NEFT999 18/09/26 0.00 50000.00 54500.00"));

        ApiResult upload = client.postMultipart("/statements/upload", Map.of("accountId", accountId), "file", "statement.pdf", pdf);
        assertThat(upload.status()).isEqualTo(200);
        String statementId = upload.data().get("id").asText();
        assertThat(upload.data().get("status").asText()).isEqualTo("READY_FOR_REVIEW");

        ApiResult list = client.get("/statements");
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.data()).hasSize(1);

        ApiResult get = client.get("/statements/" + statementId);
        assertThat(get.status()).isEqualTo(200);
        assertThat(get.data().get("periodStart").asText()).isEqualTo("2026-09-15");
        assertThat(get.data().get("periodEnd").asText()).isEqualTo("2026-09-18");

        ApiResult transactions = client.get("/statements/" + statementId + "/transactions");
        assertThat(transactions.status()).isEqualTo(200);
        assertThat(transactions.data()).hasSize(2);
        String stagingId = transactions.data().get(0).get("id").asText();

        ApiResult update = client.put(
                "/statements/" + statementId + "/transactions/" + stagingId,
                Map.of(
                        "transactionDate", "2026-09-15",
                        "amount", 550,
                        "transactionType", "EXPENSE"));
        assertThat(update.status()).isEqualTo(200);
        assertThat(update.data().get("amount").asDouble()).isEqualTo(550.0);
        assertThat(update.data().get("reviewStatus").asText()).isEqualTo("EDITED");

        ApiResult confirm = client.post("/statements/" + statementId + "/confirm", Map.of());
        assertThat(confirm.status()).isEqualTo(200);
        assertThat(confirm.data().get("statement").get("status").asText()).isEqualTo("IMPORTED");
        assertThat(confirm.data().get("importedTransactionIds")).hasSize(2);

        ApiResult confirmedStatement = client.get("/statements/" + statementId);
        assertThat(confirmedStatement.data().get("status").asText()).isEqualTo("IMPORTED");

        ApiResult ledgerTransactions = client.get("/transactions");
        assertThat(ledgerTransactions.data()).hasSize(2);
        boolean anyFromStatement = false;
        for (var tx : ledgerTransactions.data()) {
            if (tx.get("source").asText().equals("STATEMENT")) {
                anyFromStatement = true;
            }
        }
        assertThat(anyFromStatement).isTrue();
    }

    @Test
    void repeatedConfirmIsIdempotentAndNeverImportsTwice() {
        byte[] pdf = PdfFixtures.statementPdf(
                "HDFC BANK Statement of Account",
                List.of("15/09/26 COFFEE SHOP REF001 15/09/26 200.00 0.00 4800.00"));
        String statementId = client.postMultipart("/statements/upload", Map.of("accountId", accountId), "file", "statement.pdf", pdf)
                .data().get("id").asText();

        ApiResult first = client.post("/statements/" + statementId + "/confirm", Map.of());
        ApiResult second = client.post("/statements/" + statementId + "/confirm", Map.of());

        assertThat(first.status()).isEqualTo(200);
        assertThat(second.status()).isEqualTo(200);
        assertThat(second.data().get("importedTransactionIds")).isEqualTo(first.data().get("importedTransactionIds"));

        ApiResult ledgerTransactions = client.get("/transactions");
        assertThat(ledgerTransactions.data()).hasSize(1);
    }

    @Test
    void confirmIsRejectedUnlessTheStatementIsReadyForReview() {
        byte[] scanned = PdfFixtures.scannedLookingPdf();
        String statementId = client.postMultipart("/statements/upload", Map.of("accountId", accountId), "file", "scanned.pdf", scanned)
                .data().get("id").asText();

        ApiResult statement = client.get("/statements/" + statementId);
        assertThat(statement.data().get("status").asText()).isEqualTo("FAILED");

        ApiResult confirm = client.post("/statements/" + statementId + "/confirm", Map.of());
        assertThat(confirm.status()).isEqualTo(400);
    }

    @Test
    void retryReprocessesAFailedStatementAndRejectsRetryingANonFailedOne() {
        byte[] scanned = PdfFixtures.scannedLookingPdf();
        String statementId = client.postMultipart("/statements/upload", Map.of("accountId", accountId), "file", "scanned.pdf", scanned)
                .data().get("id").asText();
        assertThat(client.get("/statements/" + statementId).data().get("status").asText()).isEqualTo("FAILED");

        ApiResult retry = client.post("/statements/" + statementId + "/retry", Map.of());
        assertThat(retry.status()).isEqualTo(200);
        // Same unreadable content, so it fails again - the point of this assertion is that retry
        // actually re-ran processing (not just returned the old state), landing back on FAILED cleanly.
        assertThat(retry.data().get("status").asText()).isEqualTo("FAILED");

        byte[] goodPdf = PdfFixtures.statementPdf("HDFC BANK", List.of("15/09/26 TEST REF 15/09/26 100.00 0.00 900.00"));
        String readyStatementId = client.postMultipart("/statements/upload", Map.of("accountId", accountId), "file", "ok.pdf", goodPdf)
                .data().get("id").asText();
        ApiResult retryNonFailed = client.post("/statements/" + readyStatementId + "/retry", Map.of());
        assertThat(retryNonFailed.status()).isEqualTo(400);
    }

    @Test
    void unsupportedFormatFailsCleanlyAndUploadingTheSameFileTwiceIsIdempotent() {
        byte[] unsupported = PdfFixtures.statementPdf(
                "Totally Unknown Bank", List.of("this line matches no known bank's row format at all"));

        ApiResult firstUpload = client.postMultipart("/statements/upload", Map.of("accountId", accountId), "file", "unknown.pdf", unsupported);
        assertThat(firstUpload.status()).isEqualTo(200);
        assertThat(firstUpload.data().get("status").asText()).isEqualTo("FAILED");
        String statementId = firstUpload.data().get("id").asText();

        // file_hash gives upload-level idempotency - re-uploading the identical bytes returns
        // the same statement rather than creating a second row (database-design.md).
        ApiResult secondUpload = client.postMultipart("/statements/upload", Map.of("accountId", accountId), "file", "unknown.pdf", unsupported);
        assertThat(secondUpload.data().get("id").asText()).isEqualTo(statementId);

        ApiResult list = client.get("/statements");
        assertThat(list.data()).hasSize(1);
    }

    @Test
    void rejectsAnUploadThatIsNotAPdfAtAll() {
        ApiResult upload = client.postMultipart(
                "/statements/upload", Map.of("accountId", accountId), "file", "not-a-pdf.pdf", PdfFixtures.notAPdf());
        assertThat(upload.status()).isEqualTo(415);
        assertThat(upload.errorCode()).isEqualTo("UNSUPPORTED_FILE");
    }

    private static final String SWIGGY = "15/09/26 SWIGGY BANGALORE UPI123456 15/09/26 500.00 0.00 4500.00";
    private static final String SALARY = "18/09/26 SALARY CREDIT ACME CORP NEFT999 18/09/26 0.00 50000.00 54500.00";
    private static final String AMAZON = "20/09/26 AMAZON PAY INDIA UPI777777 20/09/26 250.00 0.00 54250.00";

    private String upload(String fileName, String... rows) {
        ApiResult upload = client.postMultipart(
                "/statements/upload", Map.of("accountId", accountId), "file", fileName,
                PdfFixtures.statementPdf("HDFC BANK Statement of Account", List.of(rows)));
        assertThat(upload.data().get("status").asText()).isEqualTo("READY_FOR_REVIEW");
        return upload.data().get("id").asText();
    }

    private String duplicateStatusOf(String statementId, int index) {
        return client.get("/statements/" + statementId + "/transactions").data().get(index).get("duplicateStatus").asText();
    }

    @Test
    void anOverlappingStatementsRepeatedRowsAreFlaggedAndSkippedOnConfirm() {
        String first = upload("first.pdf", SWIGGY, SALARY);
        // Staged while the first is still unconfirmed, so nothing can be flagged yet.
        String overlapping = upload("overlapping.pdf", SWIGGY, SALARY, AMAZON);
        assertThat(duplicateStatusOf(overlapping, 0)).isEqualTo("NOT_DUPLICATE");

        client.post("/statements/" + first + "/confirm", Map.of());

        ApiResult recheck = client.post("/statements/" + overlapping + "/duplicates/recheck", Map.of());
        assertThat(recheck.status()).isEqualTo(200);
        long duplicates = 0;
        for (var row : recheck.data()) {
            if (row.get("duplicateStatus").asText().equals("DUPLICATE")) {
                duplicates++;
                assertThat(row.get("duplicateReason").asText()).isEqualTo("EXACT_REFERENCE");
            }
        }
        assertThat(duplicates).isEqualTo(2);

        ApiResult confirm = client.post("/statements/" + overlapping + "/confirm", Map.of());
        assertThat(confirm.data().get("importedTransactionIds")).hasSize(1);
        assertThat(confirm.data().get("skippedDuplicateCount").asInt()).isEqualTo(2);
        assertThat(client.get("/transactions").data()).hasSize(3);
    }

    @Test
    void aFlaggedDuplicateCanBeKeptAnywayAndThenImports() {
        String first = upload("first.pdf", SWIGGY);
        client.post("/statements/" + first + "/confirm", Map.of());

        String second = upload("second.pdf", SWIGGY, AMAZON);
        assertThat(rowFor(second, "500").get("duplicateStatus").asText()).isEqualTo("DUPLICATE");

        String stagingId = rowFor(second, "500").get("id").asText();
        ApiResult keep = client.post("/statements/" + second + "/transactions/" + stagingId + "/keep-duplicate", Map.of());
        assertThat(keep.status()).isEqualTo(200);
        assertThat(keep.data().get("duplicateStatus").asText()).isEqualTo("NOT_DUPLICATE");
        assertThat(keep.data().get("duplicateOverridden").asBoolean()).isTrue();

        ApiResult confirm = client.post("/statements/" + second + "/confirm", Map.of());
        assertThat(confirm.data().get("importedTransactionIds")).hasSize(2);
        assertThat(confirm.data().get("skippedDuplicateCount").asInt()).isZero();
    }

    private com.fasterxml.jackson.databind.JsonNode rowFor(String statementId, String amount) {
        for (var row : client.get("/statements/" + statementId + "/transactions").data()) {
            if (row.get("amount").asDouble() == Double.parseDouble(amount)) {
                return row;
            }
        }
        throw new AssertionError("no staged row with amount " + amount);
    }

    @Test
    void aCategoryChosenOnceIsRememberedForTheSamePayeeAcrossRowsAndStatements() {
        String category = client.get("/categories").data().get(0).get("id").asText();
        String first = upload(
                "first.pdf",
                "15/09/26 SWIGGY BANGALORE UPI111111 15/09/26 500.00 0.00 4500.00",
                "16/09/26 SWIGGY BANGALORE UPI222222 16/09/26 300.00 0.00 4200.00",
                "17/09/26 AMAZON PAY INDIA UPI333333 17/09/26 250.00 0.00 3950.00");
        assertThat(rowFor(first, "300").get("suggestedCategoryId").isNull()).isTrue();

        client.put(
                "/statements/" + first + "/transactions/" + rowFor(first, "500").get("id").asText(),
                Map.of("transactionDate", "2026-09-15", "amount", 500, "categoryId", category, "transactionType", "EXPENSE"));

        // Editing a row must not move it: the list stays newest-first in statement order.
        var order = client.get("/statements/" + first + "/transactions").data();
        assertThat(order.get(0).get("amount").asDouble()).isEqualTo(250.0);
        assertThat(order.get(1).get("amount").asDouble()).isEqualTo(300.0);
        assertThat(order.get(2).get("amount").asDouble()).isEqualTo(500.0);

        // The second Swiggy row of the same statement picks it up straight away; Amazon is untouched.
        assertThat(rowFor(first, "300").get("suggestedCategoryId").asText()).isEqualTo(category);
        assertThat(rowFor(first, "250").get("suggestedCategoryId").isNull()).isTrue();

        // And the next statement for the same payee is pre-filled when it is staged.
        String second = upload("second.pdf", "20/09/26 SWIGGY BANGALORE UPI444444 20/09/26 410.00 0.00 3540.00");
        assertThat(rowFor(second, "410").get("suggestedCategoryId").asText()).isEqualTo(category);
    }
}
