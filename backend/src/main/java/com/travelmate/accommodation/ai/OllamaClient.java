package com.travelmate.accommodation.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelmate.accommodation.exception.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Thin client for the local Ollama server: POST {baseUrl}/api/chat with stream=false and format=json.
 * Every failure (connection refused, timeout, HTTP error, empty answer) becomes an {@link AiUnavailableException}
 * so callers can fall back gracefully.
 */
@Slf4j
@Component
public class OllamaClient {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaClient(OllamaProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        // Buffering sends a Content-Length header instead of a chunked body.
        this.restClient = builder.baseUrl(properties.getBaseUrl())
                .requestFactory(new BufferingClientHttpRequestFactory(factory))
                .build();
    }

    /** Sends a system + user prompt and returns the JSON text produced by the model. */
    public OllamaReply chatJson(String systemPrompt, String userPrompt) {
        if (!properties.isEnabled()) {
            throw new AiUnavailableException("AI is disabled by configuration");
        }
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "stream", false,
                "format", "json",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "options", Map.of("temperature", properties.getTemperature()));

        long start = System.currentTimeMillis();
        try {
            ChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);
            long duration = System.currentTimeMillis() - start;

            if (response == null || response.message() == null || response.message().content() == null
                    || response.message().content().isBlank()) {
                throw new AiUnavailableException("Empty answer from Ollama");
            }
            log.info("Ollama [{}] answered in {} ms (prompt {} chars, prompt tokens {}, output tokens {})",
                    properties.getModel(), duration, userPrompt.length(),
                    response.promptEvalCount(), response.evalCount());
            return new OllamaReply(response.message().content(), properties.getModel(), duration);
        } catch (RestClientException e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Ollama call failed after {} ms: {}", duration, e.getMessage());
            throw new AiUnavailableException("Local AI (Ollama) is unreachable or timed out", e);
        }
    }

    /** Quick health check used by GET /api/accommodations/ai/status. */
    public boolean isAvailable() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            restClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    public String model() {
        return properties.getModel();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(String model,
                        Message message,
                        Boolean done,
                        @JsonProperty("prompt_eval_count") Integer promptEvalCount,
                        @JsonProperty("eval_count") Integer evalCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }
}
