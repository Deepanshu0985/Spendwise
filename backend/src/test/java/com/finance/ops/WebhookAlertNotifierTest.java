package com.finance.ops;

import com.finance.infrastructure.ops.WebhookAlertNotifier;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class WebhookAlertNotifierTest {

    private record Received(String title, String contentType, String body) {
    }

    private HttpServer server;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private int status = 200;
    private String url;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.add(new Received(exchange.getRequestHeaders().getFirst("Title"), exchange.getRequestHeaders().getFirst("Content-Type"), body));
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void plainFormatSendsTheMessageAsTheBodyAndTheTitleAsAHeader() {
        new WebhookAlertNotifier(url, "plain").send("Spendwise: server errors", "8 of 40 requests failed");
        assertThat(received).hasSize(1);
        assertThat(received.get(0).title()).isEqualTo("Spendwise: server errors");
        assertThat(received.get(0).body()).isEqualTo("8 of 40 requests failed");
    }

    @Test
    void slackAndDiscordGetTheirOwnJsonShape() {
        new WebhookAlertNotifier(url, "slack").send("T", "M");
        new WebhookAlertNotifier(url, "discord").send("T", "M");
        assertThat(received.get(0).body()).contains("\"text\"").contains("M");
        assertThat(received.get(0).contentType()).contains("json");
        assertThat(received.get(1).body()).contains("\"content\"").contains("M");
    }

    @Test
    void noUrlMeansOnlyALogLine() {
        new WebhookAlertNotifier("", "plain").send("T", "M");
        assertThat(received).isEmpty();
    }

    @Test
    void aFailingOrUnreachableWebhookNeverThrowsIntoTheCaller() {
        status = 500;
        assertThatCode(() -> new WebhookAlertNotifier(url, "plain").send("T", "M")).doesNotThrowAnyException();
        assertThatCode(() -> new WebhookAlertNotifier("http://127.0.0.1:1/none", "plain").send("T", "M")).doesNotThrowAnyException();
    }
}
