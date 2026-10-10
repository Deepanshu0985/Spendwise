package com.finance.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.application.ai.AiModelClient;
import com.finance.application.ai.ModelResult;
import com.finance.application.exception.AiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Mistral behind the AiModelClient port (D-03). The API key is read from configuration only, sent only in the
 * Authorization header, and never logged or put in an error message; neither is the request or response text. Calls have
 * a timeout and one bounded retry for a transient failure.
 */
@Component
public class MistralModelClient implements AiModelClient {

    private static final Logger log = LoggerFactory.getLogger(MistralModelClient.class);
    private static final int MAX_ATTEMPTS = 2;

    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MistralModelClient(
            @Value("${ai.enabled:false}") boolean enabled,
            @Value("${MISTRAL_API_KEY:}") String apiKey,
            @Value("${ai.mistral.model:mistral-small-latest}") String model,
            @Value("${ai.mistral.base-url:https://api.mistral.ai}") String baseUrl,
            @Value("${ai.mistral.timeout:30s}") Duration timeout,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public boolean isAvailable() {
        return enabled && !apiKey.isEmpty();
    }

    @Override
    public ModelResult complete(String systemPrompt, String userContent) {
        if (!isAvailable()) {
            throw new AiUnavailableException("AI suggestions are not available right now.");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "max_tokens", 1800,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)));

        // Serialised up front so the request carries a fixed Content-Length rather than being streamed chunked.
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new AiUnavailableException("AI suggestions are not available right now.");
        }

        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String response = restClient.post()
                        .uri("/v1/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);
                return parse(response);
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // 4xx other than a rate limit will not improve by retrying (bad key, bad request).
                log.warn("AI provider rejected the request: HTTP {}", e.getStatusCode().value());
                if (e.getStatusCode().value() != 429) {
                    throw new AiUnavailableException("AI suggestions are not available right now.");
                }
                last = e;
            } catch (RuntimeException e) {
                log.warn("AI provider call failed (attempt {}): {}", attempt, e.getClass().getSimpleName());
                last = e;
            }
            if (attempt < MAX_ATTEMPTS) {
                sleepBriefly();
            }
        }
        throw new AiUnavailableException("AI suggestions are not available right now.");
    }

    private ModelResult parse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("choices").path(0).path("message").path("content").asText(null);
            if (text == null || text.isBlank()) {
                throw new AiUnavailableException("AI suggestions are not available right now.");
            }
            return new ModelResult(text, root.path("usage").path("prompt_tokens").asInt(0), root.path("usage").path("completion_tokens").asInt(0));
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("AI suggestions are not available right now.");
        }
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
