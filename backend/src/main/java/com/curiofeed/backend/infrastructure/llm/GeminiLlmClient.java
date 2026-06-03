package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GENERATE_PATH = "/v1beta/models/{model}:generateContent";

    /**
     * 모든 GeminiLlmClient 인스턴스가 공유하는 뮤텍스.
     * 한 번에 하나의 Gemini 호출만 진행되도록 보장해 429를 원천 차단한다.
     */
    private static final Semaphore CALL_LOCK = new Semaphore(1, true);

    /** Free Plan RPM(15회/분)을 준수하기 위한 최소 호출 간격 (4초) */
    private static final long MIN_INTERVAL_MS = 4000L;
    private static long lastCallTimeMs = 0;

    /** 429 발생 시 최대 재시도 횟수 */
    private static final int MAX_RATE_LIMIT_RETRIES = 2;
    private static final long BACKOFF_MS = 20_000L;

    private final String apiKey;
    private final String model;
    private final Double temperature;
    private final RestClient restClient;

    public GeminiLlmClient(GeminiProperties properties, String model, RestClient.Builder restClientBuilder) {
        this.apiKey = properties.apiKey();
        this.model = model;
        this.temperature = properties.temperature();
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    @Override
    public String generate(String prompt) {
        return generate(prompt, null);
    }

    @Override
    public String generate(String prompt, Map<String, Object> schema) {
        try {
            CALL_LOCK.acquire();
            long now = System.currentTimeMillis();
            long timeSinceLastCall = now - lastCallTimeMs;
            if (timeSinceLastCall < MIN_INTERVAL_MS) {
                sleepUninterruptibly(MIN_INTERVAL_MS - timeSinceLastCall);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmClientException("Interrupted while waiting for Gemini call lock", e);
        }
        try {
            for (int attempt = 1; attempt <= MAX_RATE_LIMIT_RETRIES + 1; attempt++) {
                try {
                    return doGenerate(prompt, schema);
                } catch (RateLimitException e) {
                    if (attempt > MAX_RATE_LIMIT_RETRIES) {
                        throw new LlmClientException(
                                "Gemini rate limit exceeded after " + MAX_RATE_LIMIT_RETRIES + " retries (model=" + model + ")");
                    }
                    log.warn("[GeminiLlmClient] 429 received — waiting {}s before retry {}/{}  model={}",
                            BACKOFF_MS / 1000, attempt, MAX_RATE_LIMIT_RETRIES, model);
                    sleepUninterruptibly(BACKOFF_MS);
                }
            }
            throw new LlmClientException("Gemini generate unreachable");
        } finally {
            lastCallTimeMs = System.currentTimeMillis();
            CALL_LOCK.release();
        }
    }

    private String doGenerate(String prompt, Map<String, Object> schema) {
        GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig(
                temperature,
                schema != null ? "application/json" : null,
                schema != null ? sanitizeSchema(schema) : null
        );
        GeminiRequest request = new GeminiRequest(
                List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))),
                config
        );

        try {
            GeminiResponse response = restClient.post()
                    .uri(builder -> builder
                            .path(GENERATE_PATH)
                            .queryParam("key", apiKey)
                            .build(model))
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 429,
                            (req, res) -> { throw new RateLimitException("HTTP 429"); })
                    .onStatus(status -> status.isError(),
                            (req, res) -> { throw new LlmClientException("Gemini call failed: HTTP " + res.getStatusCode()); })
                    .body(GeminiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new LlmClientException("Gemini call failed: empty response");
            }
            List<GeminiResponse.Part> parts = response.candidates().get(0).content().parts();
            if (parts == null || parts.isEmpty()) {
                throw new LlmClientException("Gemini call failed: no content parts");
            }
            return parts.get(0).text();

        } catch (RateLimitException | LlmClientException e) {
            throw e;
        } catch (RestClientException e) {
            throw new LlmClientException("Gemini call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gemini responseSchema는 additionalProperties를 지원하지 않으므로 재귀적으로 제거한다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeSchema(Map<String, Object> schema) {
        Map<String, Object> sanitized = new LinkedHashMap<>(schema);
        sanitized.remove("additionalProperties");

        if (sanitized.get("properties") instanceof Map<?, ?> props) {
            Map<String, Object> sanitizedProps = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : props.entrySet()) {
                Object val = entry.getValue();
                sanitizedProps.put((String) entry.getKey(),
                        val instanceof Map<?, ?> ? sanitizeSchema((Map<String, Object>) val) : val);
            }
            sanitized.put("properties", sanitizedProps);
        }
        if (sanitized.get("items") instanceof Map<?, ?> items) {
            sanitized.put("items", sanitizeSchema((Map<String, Object>) items));
        }
        return sanitized;
    }

    private void sleepUninterruptibly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LlmClientException("Interrupted during Gemini rate-limit backoff", ie);
        }
    }

    /** 429 전용 내부 예외 */
    private static class RateLimitException extends RuntimeException {
        RateLimitException(String message) { super(message); }
    }
}
