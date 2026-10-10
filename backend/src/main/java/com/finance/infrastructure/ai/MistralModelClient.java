package com.finance.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.application.ai.AiModelClient;
import com.finance.application.ai.ChatMessage;
import com.finance.application.ai.ChatResult;
import com.finance.application.ai.ModelResult;
import com.finance.application.ai.ToolCall;
import com.finance.application.ai.ToolSpec;
import com.finance.application.exception.AiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final String UNAVAILABLE = "AI is not available right now.";

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
    public String modelName() {
        return model;
    }

    @Override
    public boolean isAvailable() {
        return enabled && !apiKey.isEmpty();
    }

    @Override
    public ModelResult complete(String systemPrompt, String userContent) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("max_tokens", 1800);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)));
        JsonNode root = post(body);
        String text = root.path("choices").path(0).path("message").path("content").asText(null);
        if (text == null || text.isBlank()) {
            throw new AiUnavailableException(UNAVAILABLE);
        }
        return new ModelResult(text, tokens(root, "prompt_tokens"), tokens(root, "completion_tokens"));
    }

    @Override
    public ChatResult chat(String systemPrompt, List<ChatMessage> messages, List<ToolSpec> tools) {
        List<Map<String, Object>> wire = new ArrayList<>();
        wire.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage message : messages) {
            wire.add(toWire(message));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("max_tokens", 1200);
        body.put("messages", wire);
        if (!tools.isEmpty()) {
            body.put("tools", tools.stream().map(tool -> Map.of(
                    "type", "function",
                    "function", Map.of("name", tool.name(), "description", tool.description(), "parameters", tool.parameters()))).toList());
            body.put("tool_choice", "auto");
        }
        JsonNode root = post(body);
        JsonNode message = root.path("choices").path(0).path("message");
        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode call : message.path("tool_calls")) {
            String arguments = call.path("function").path("arguments").isTextual()
                    ? call.path("function").path("arguments").asText() : call.path("function").path("arguments").toString();
            calls.add(new ToolCall(call.path("id").asText(""), call.path("function").path("name").asText(""), arguments));
        }
        String content = message.path("content").isTextual() ? message.path("content").asText() : "";
        if (calls.isEmpty() && content.isBlank()) {
            throw new AiUnavailableException(UNAVAILABLE);
        }
        return new ChatResult(content, calls, tokens(root, "prompt_tokens"), tokens(root, "completion_tokens"));
    }

    private Map<String, Object> toWire(ChatMessage message) {
        Map<String, Object> wire = new LinkedHashMap<>();
        wire.put("role", message.role());
        wire.put("content", message.content() == null ? "" : message.content());
        if (!message.toolCalls().isEmpty()) {
            wire.put("tool_calls", message.toolCalls().stream().map(call -> Map.of(
                    "id", call.id(), "type", "function",
                    "function", Map.of("name", call.name(), "arguments", call.argumentsJson()))).toList());
        }
        if ("tool".equals(message.role())) {
            wire.put("tool_call_id", message.toolCallId());
            wire.put("name", message.toolName());
        }
        return wire;
    }

    private static int tokens(JsonNode root, String field) {
        return root.path("usage").path(field).asInt(0);
    }

    /** One request with a timeout and a single retry for a transient failure; the text of neither side is ever logged. */
    private JsonNode post(Map<String, Object> body) {
        if (!isAvailable()) {
            throw new AiUnavailableException(UNAVAILABLE);
        }
        // Serialised up front so the request carries a fixed Content-Length rather than being streamed chunked.
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new AiUnavailableException(UNAVAILABLE);
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String response = restClient.post()
                        .uri("/v1/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);
                return objectMapper.readTree(response);
            } catch (HttpClientErrorException e) {
                // 4xx other than a rate limit will not improve by retrying (bad key, bad request).
                log.warn("AI provider rejected the request: HTTP {}", e.getStatusCode().value());
                if (e.getStatusCode().value() != 429) {
                    throw new AiUnavailableException(UNAVAILABLE);
                }
            } catch (JsonProcessingException e) {
                throw new AiUnavailableException(UNAVAILABLE);
            } catch (RuntimeException e) {
                log.warn("AI provider call failed (attempt {}): {}", attempt, e.getClass().getSimpleName());
            }
            if (attempt < MAX_ATTEMPTS) {
                sleepBriefly();
            }
        }
        throw new AiUnavailableException(UNAVAILABLE);
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
