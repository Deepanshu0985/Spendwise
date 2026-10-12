package com.finance.infrastructure.ops;

import com.finance.application.ops.AlertNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * Posts an alert to a webhook the operator chooses (ALERT_WEBHOOK_URL): ntfy.sh works with no account (plain text, the
 * default), Slack and Discord take a small JSON body (ALERT_WEBHOOK_FORMAT). With no URL set the alert is only logged, so
 * the code path is the same everywhere. A failed delivery is logged by exception type and never retried in a loop or allowed
 * to affect the request that raised it.
 */
@Component
public class WebhookAlertNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookAlertNotifier.class);

    private final String url;
    private final String format;
    private final RestClient restClient;

    public WebhookAlertNotifier(
            @Value("${ALERT_WEBHOOK_URL:}") String url, @Value("${ALERT_WEBHOOK_FORMAT:plain}") String format) {
        this.url = url == null ? "" : url.trim();
        this.format = format == null ? "plain" : format.trim().toLowerCase();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void send(String title, String message) {
        log.warn("ALERT: {} - {}", title, message);
        if (url.isEmpty()) {
            return;
        }
        try {
            switch (format) {
                case "slack" -> restClient.post().uri(url).header("Content-Type", "application/json")
                        .body(Map.of("text", "*" + title + "*\n" + message)).retrieve().toBodilessEntity();
                case "discord" -> restClient.post().uri(url).header("Content-Type", "application/json")
                        .body(Map.of("content", "**" + title + "**\n" + message)).retrieve().toBodilessEntity();
                default -> restClient.post().uri(url).header("Title", title).header("Priority", "high")
                        .body(message).retrieve().toBodilessEntity();
            }
        } catch (RuntimeException e) {
            log.warn("Could not deliver the alert: {}", e.getClass().getSimpleName());
        }
    }
}
