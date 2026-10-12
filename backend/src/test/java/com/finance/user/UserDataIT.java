package com.finance.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.support.ApiResult;
import com.finance.support.FakeGoogleIdentityVerifier;
import com.finance.support.PdfFixtures;
import com.finance.support.TestData;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** A person's right to take their data and to have it erased, end to end, including that nobody else's data is touched. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Import(UserDataIT.FakeGoogleConfig.class)
class UserDataIT {

    @TestConfiguration
    static class FakeGoogleConfig {
        @Bean
        @Primary
        FakeGoogleIdentityVerifier fakeGoogleIdentityVerifier() {
            return new FakeGoogleIdentityVerifier();
        }
    }

    private static final String PASSWORD = "correcthorse123";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeGoogleIdentityVerifier fakeGoogle;

    private String email;
    private TestHttpClient client;
    private String userId;

    @BeforeEach
    void setUp() {
        fakeGoogle.configured = true;
        email = TestData.uniqueEmail("data");
        client = signedIn(email);
        userId = client.get("/users/me").data().get("id").asText();
        fillWithData(client);
    }

    private TestHttpClient signedIn(String address) {
        TestHttpClient c = new TestHttpClient(port);
        c.primeCsrfToken();
        c.post("/auth/register", Map.of("email", address, "password", PASSWORD, "fullName", "Data Test"));
        c.post("/auth/login", Map.of("email", address, "password", PASSWORD));
        return c;
    }

    /** One of everything the product stores for a person, including a real uploaded statement file. */
    private void fillWithData(TestHttpClient c) {
        String accountId = c.post("/accounts", Map.of("name", "Bank", "accountType", "BANK", "currency", "INR")).data().get("id").asText();
        String categoryId = c.post("/categories", Map.of("name", "My own category", "categoryType", "EXPENSE")).data().get("id").asText();
        c.post("/transactions", Map.of(
                "accountId", accountId, "transactionDate", LocalDate.now().toString(), "amount", 120, "currency", "INR",
                "description", "tea and snacks", "transactionType", "EXPENSE", "categoryId", categoryId));
        c.post("/budgets", Map.of(
                "name", "Monthly", "periodType", "MONTHLY", "totalLimit", 5000, "currency", "INR",
                "categoryLimits", List.of(Map.of("categoryId", categoryId, "limitAmount", 1000))));
        c.post("/goals", Map.of("name", "Trip", "targetAmount", 10000, "currentAmount", 500, "currency", "INR"));

        byte[] pdf = PdfFixtures.statementPdf(
                "HDFC BANK Statement of Account",
                List.of("15/09/26 SWIGGY BANGALORE UPI123456 15/09/26 500.00 0.00 4500.00"));
        ApiResult upload = c.postMultipart("/statements/upload", Map.of("accountId", accountId), "file", "statement.pdf", pdf);
        assertThat(upload.status()).isEqualTo(200);
        c.post("/statements/" + upload.data().get("id").asText() + "/confirm", Map.of());

        // A second statement is left awaiting review, so its file is still held (the confirmed one's was removed on import).
        byte[] second = PdfFixtures.statementPdf(
                "HDFC BANK Statement of Account",
                List.of("20/09/26 ZOMATO BANGALORE UPI777777 20/09/26 300.00 0.00 4200.00"));
        assertThat(c.postMultipart("/statements/upload", Map.of("accountId", accountId), "file", "second.pdf", second).status()).isEqualTo(200);
    }

    private Map<String, String> deleteBody(String password, String confirmation) {
        return password == null ? Map.of("confirmation", confirmation) : Map.of("password", password, "confirmation", confirmation);
    }

    private long storedFilesOf(String id) throws IOException {
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "spendwise-it-storage");
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).filter(path -> path.toString().contains(id)).count();
        }
    }

    @Test
    void theExportHoldsTheirOwnDataAndNothingInternalOrAnyoneElses() throws Exception {
        TestHttpClient stranger = signedIn(TestData.uniqueEmail("stranger"));
        stranger.post("/goals", Map.of("name", "Strangers secret goal", "targetAmount", 777, "currency", "INR"));

        ApiResult export = client.get("/users/me/export");
        assertThat(export.status()).isEqualTo(200);
        assertThat(export.headers().firstValue("Content-Disposition").orElse("")).contains("attachment").contains("spendwise-data-");
        assertThat(export.headers().firstValue("Cache-Control").orElse("")).contains("no-store");

        JsonNode data = export.body();
        assertThat(data.get("formatVersion").asInt()).isEqualTo(1);
        assertThat(data.get("profile").get("email").asText()).isEqualTo(email);
        assertThat(data.get("profile").get("signInMethods").get(0).asText()).isEqualTo("password");
        assertThat(data.get("accounts")).hasSize(1);
        assertThat(data.get("transactions").size()).isGreaterThanOrEqualTo(2); // the typed one and the confirmed statement row
        assertThat(data.get("goals")).hasSize(1);
        assertThat(data.get("budgets")).hasSize(1);
        assertThat(data.get("statements")).hasSize(2);
        assertThat(data.get("statementRows")).hasSize(2);
        assertThat(data.get("categories").toString()).contains("My own category");

        String raw = MAPPER.writeValueAsString(data);
        assertThat(raw).doesNotContain("password_hash").doesNotContain("passwordHash").doesNotContain("storage_key").doesNotContain("Strangers secret goal");
        assertThat(raw).doesNotContain(userId); // the internal user id is not exported
    }

    @Test
    void exportNeedsASession() {
        TestHttpClient anonymous = new TestHttpClient(port);
        anonymous.primeCsrfToken();
        assertThat(anonymous.get("/users/me/export").status()).isEqualTo(401);
    }

    @Test
    void deletingTheAccountRemovesEverythingAndLeavesOthersAlone() throws Exception {
        TestHttpClient bystander = signedIn(TestData.uniqueEmail("bystander"));
        fillWithData(bystander);
        String bystanderId = bystander.get("/users/me").data().get("id").asText();
        long bystanderFiles = storedFilesOf(bystanderId);
        assertThat(storedFilesOf(userId)).as("the confirmed statement's file is already gone; the one awaiting review remains").isEqualTo(1);

        ApiResult deleted = client.post("/users/me/delete", deleteBody(PASSWORD, "DELETE"));
        assertThat(deleted.status()).isEqualTo(204);
        assertThat(deleted.headers().firstValue("Set-Cookie").orElse("")).contains("Max-Age=0");

        // The foreign keys refuse to let a user row go while any other row still points at it, so a missing user proves
        // every table was emptied (the app's own role cannot see other rows through row security to count them).
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email)).isZero();
        assertThat(storedFilesOf(userId)).as("uploaded statement file removed").isZero();

        assertThat(client.get("/users/me").status()).as("the session ended with the account").isEqualTo(401);
        TestHttpClient again = new TestHttpClient(port);
        again.primeCsrfToken();
        assertThat(again.post("/auth/login", Map.of("email", email, "password", PASSWORD)).status()).isEqualTo(401);

        // The bystander is untouched: login, data and file all still there.
        assertThat(bystander.get("/accounts").data()).hasSize(1);
        assertThat(bystander.get("/goals").data()).hasSize(1);
        assertThat(storedFilesOf(bystanderId)).isEqualTo(bystanderFiles);
    }

    @Test
    void theSameEmailCanRegisterAgainAfterDeletion() {
        assertThat(client.post("/users/me/delete", deleteBody(PASSWORD, "DELETE")).status()).isEqualTo(204);
        TestHttpClient fresh = new TestHttpClient(port);
        fresh.primeCsrfToken();
        assertThat(fresh.post("/auth/register", Map.of("email", email, "password", PASSWORD, "fullName", "Back Again")).status()).isEqualTo(200);
        assertThat(fresh.post("/auth/login", Map.of("email", email, "password", PASSWORD)).status()).isEqualTo(200);
        assertThat(fresh.get("/goals").data()).isEmpty();
    }

    @Test
    void aWrongPasswordOrMissingConfirmationDeletesNothing() {
        ApiResult wrongPassword = client.post("/users/me/delete", deleteBody("not-my-password", "DELETE"));
        assertThat(wrongPassword.status()).isEqualTo(400);
        assertThat(wrongPassword.errorCode()).isEqualTo("VALIDATION_ERROR");

        assertThat(client.post("/users/me/delete", deleteBody(PASSWORD, "delete")).status()).as("case matters").isEqualTo(400);
        assertThat(client.post("/users/me/delete", Map.of("password", PASSWORD)).status()).as("confirmation required").isEqualTo(400);
        assertThat(client.post("/users/me/delete", deleteBody(null, "DELETE")).status()).as("password required").isEqualTo(400);

        assertThat(client.get("/users/me").status()).isEqualTo(200);
        assertThat(client.get("/goals").data()).hasSize(1);
    }

    @Test
    void deletionNeedsASessionAndTheCsrfToken() {
        TestHttpClient anonymous = new TestHttpClient(port);
        anonymous.primeCsrfToken();
        assertThat(anonymous.post("/users/me/delete", deleteBody(PASSWORD, "DELETE")).status()).isEqualTo(401);
        assertThat(client.postWithoutCsrf("/users/me/delete", deleteBody(PASSWORD, "DELETE")).status()).isEqualTo(403);
        assertThat(client.get("/users/me").status()).isEqualTo(200);
    }

    @Test
    void aGoogleOnlyAccountProvesItselfWithItsOwnFreshGoogleCredential() {
        String googleEmail = TestData.uniqueEmail("gdel");
        String subject = "sub-" + UUID.randomUUID();
        TestHttpClient google = new TestHttpClient(port);
        google.primeCsrfToken();
        google.post("/auth/google", Map.of("credential", fakeGoogle.tokenFor(subject, googleEmail, "Gina Google")));
        google.post("/goals", Map.of("name", "Trip", "targetAmount", 100, "currency", "INR"));

        assertThat(google.get("/users/me").data().get("hasPassword").asBoolean()).isFalse();
        assertThat(google.get("/users/me").data().get("googleLinked").asBoolean()).isTrue();

        assertThat(google.post("/users/me/delete", deleteBody(null, "DELETE")).status()).as("no proof").isEqualTo(400);
        String otherAccount = fakeGoogle.tokenFor("sub-" + UUID.randomUUID(), TestData.uniqueEmail("other"), "Someone Else");
        assertThat(google.post("/users/me/delete", Map.of("confirmation", "DELETE", "googleCredential", otherAccount)).status())
                .as("a different Google account is not proof")
                .isEqualTo(400);
        assertThat(google.get("/users/me").status()).isEqualTo(200);

        String mine = fakeGoogle.tokenFor(subject, googleEmail, "Gina Google");
        assertThat(google.post("/users/me/delete", Map.of("confirmation", "DELETE", "googleCredential", mine)).status()).isEqualTo(204);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, googleEmail)).isZero();
    }
}
