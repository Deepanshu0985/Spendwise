package com.finance.ops;

import com.finance.support.ApiResult;
import com.finance.support.TestHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class OpsIT {

    @LocalServerPort
    private int port;

    @Test
    void livenessAndReadinessAreSeparateAndBothPublic() {
        TestHttpClient client = new TestHttpClient(port);
        ApiResult live = client.get("/health");
        assertThat(live.status()).isEqualTo(200);
        assertThat(live.data().get("status").asText()).isEqualTo("UP");
        assertThat(live.data().has("database")).as("liveness never touches the database").isFalse();

        ApiResult ready = client.get("/health/ready");
        assertThat(ready.status()).isEqualTo(200);
        assertThat(ready.data().get("database").asText()).isEqualTo("UP");
    }
}
