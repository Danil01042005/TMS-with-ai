package ru.tms.user.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.tms.user.service.exception.ExternalApiException;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Реализация AIClient для Groq Chat Completions API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroqAIClient implements AIClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key:${GROQ_API_KEY:}}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:llama-3.1-8b-instant}")
    private String groqModel;

    @Value("${groq.api.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${groq.api.retry.backoff-ms:800}")
    private long backoffMs;

    @Value("${groq.api.rate-limit-per-minute:30}")
    private int rateLimitPerMinute;

    private final ReentrantLock rateLimitLock = new ReentrantLock();
    private final ArrayDeque<Long> callTimestampsMs = new ArrayDeque<>();

    @Override
    public String complete(String prompt) {
        if (groqApiKey == null || groqApiKey.isEmpty()) {
            throw new ExternalApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EXTERNAL_AI_NOT_CONFIGURED",
                    "AI service is not configured"
            );
        }

        enforceRateLimit();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqModel);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 4000);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("[GroqAIClient] Sending request to Groq API: url={}, model={}, attempt={}/{}",
                        groqApiUrl, groqModel, attempt, maxAttempts);
                log.debug("[GroqAIClient] Request body: {}", objectMapper.writeValueAsString(requestBody));

                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> responseEntity = restTemplate.exchange(
                        groqApiUrl,
                        HttpMethod.POST,
                        requestEntity,
                        Map.class
                );

                if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                    throw new ExternalApiException(
                            HttpStatus.BAD_GATEWAY,
                            "EXTERNAL_AI_BAD_RESPONSE",
                            "AI service returned non-success status: " + responseEntity.getStatusCode().value()
                    );
                }

                return extractNormalizedContent(responseEntity.getBody());
            } catch (JsonProcessingException e) {
                throw new ExternalApiException(HttpStatus.INTERNAL_SERVER_ERROR, "EXTERNAL_AI_SERIALIZATION_ERROR",
                        "Failed to serialize AI request", e);
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                String body = e.getResponseBodyAsString();
                boolean retryable = status == 429 || status >= 500;
                if (retryable && attempt < maxAttempts) {
                    sleepBackoff(attempt);
                    continue;
                }
                if (status == 429) {
                    throw new ExternalApiException(HttpStatus.TOO_MANY_REQUESTS, "EXTERNAL_AI_RATE_LIMIT",
                            "External AI rate limit exceeded");
                }
                throw new ExternalApiException(HttpStatus.BAD_GATEWAY, "EXTERNAL_AI_HTTP_ERROR",
                        "External AI failed with status " + status + (body == null || body.isBlank() ? "" : (": " + body)));
            } catch (ResourceAccessException e) {
                if (attempt < maxAttempts) {
                    sleepBackoff(attempt);
                    continue;
                }
                throw new ExternalApiException(HttpStatus.GATEWAY_TIMEOUT, "EXTERNAL_AI_TIMEOUT",
                        "External AI request timed out or network is unavailable", e);
            } catch (ExternalApiException e) {
                throw e;
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    sleepBackoff(attempt);
                    continue;
                }
                throw new ExternalApiException(HttpStatus.BAD_GATEWAY, "EXTERNAL_AI_UNAVAILABLE",
                        "Failed to call external AI service", e);
            }
        }

        throw new ExternalApiException(HttpStatus.BAD_GATEWAY, "EXTERNAL_AI_UNAVAILABLE", "External AI service failed");
    }

    private String extractNormalizedContent(Map<String, Object> response) {
        if (response == null) {
            throw new ExternalApiException(HttpStatus.BAD_GATEWAY, "EXTERNAL_AI_EMPTY_RESPONSE",
                    "External AI returned empty response");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new ExternalApiException(HttpStatus.BAD_GATEWAY, "EXTERNAL_AI_INVALID_RESPONSE",
                    "External AI response has no choices");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            throw new ExternalApiException(HttpStatus.BAD_GATEWAY, "EXTERNAL_AI_INVALID_RESPONSE",
                    "External AI response has no message");
        }

        String content = (String) message.get("content");
        if (content == null || content.isBlank()) {
            throw new ExternalApiException(HttpStatus.BAD_GATEWAY, "EXTERNAL_AI_INVALID_RESPONSE",
                    "External AI returned empty content");
        }

        return normalizeContent(content);
    }

    private String normalizeContent(String content) {
        String normalized = content.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```[a-zA-Z]*\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }
        return normalized.trim();
    }

    private void enforceRateLimit() {
        long now = Instant.now().toEpochMilli();
        long threshold = now - 60_000L;

        rateLimitLock.lock();
        try {
            while (!callTimestampsMs.isEmpty() && callTimestampsMs.peekFirst() < threshold) {
                callTimestampsMs.pollFirst();
            }
            if (callTimestampsMs.size() >= rateLimitPerMinute) {
                throw new ExternalApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "AI_CLIENT_RATE_LIMIT",
                        "Too many AI generation requests, please retry later"
                );
            }
            callTimestampsMs.addLast(now);
        } finally {
            rateLimitLock.unlock();
        }
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(backoffMs * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}













