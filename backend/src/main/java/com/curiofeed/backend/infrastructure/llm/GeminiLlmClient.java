package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GENERATE_PATH = "/v1beta/models/{model}:generateContent";

    /**
     * 모든 GeminiLlmClient 인스턴스가 공유하는 슬라이딩 윈도우 rate limiter.
     * 무료 티어 10 RPM 한계에 여유를 두고 8 RPM으로 제한한다.
     */
    private static final SlidingWindowRateLimiter RATE_LIMITER = new SlidingWindowRateLimiter(8, 60_000L);

    /** 429 발생 시 최대 재시도 횟수 (rate limiter가 있으므로 낮게 설정) */
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
        for (int attempt = 1; attempt <= MAX_RATE_LIMIT_RETRIES + 1; attempt++) {
            // 호출 전에 슬롯 확보 — 필요하면 대기
            RATE_LIMITER.acquire();
            try {
                return doGenerate(prompt, schema);
            } catch (RateLimitException e) {
                if (attempt > MAX_RATE_LIMIT_RETRIES) {
                    throw new LlmClientException(
                            "Gemini rate limit exceeded after " + MAX_RATE_LIMIT_RETRIES + " retries (model=" + model + ")");
                }
                log.warn("[GeminiLlmClient] 429 received despite rate limiter — waiting {}s before retry {}/{}  model={}",
                        BACKOFF_MS / 1000, attempt, MAX_RATE_LIMIT_RETRIES, model);
                sleepUninterruptibly(BACKOFF_MS);
            }
        }
        throw new LlmClientException("Gemini generate unreachable");
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

    /**
     * 슬라이딩 윈도우 방식의 rate limiter.
     * 최근 windowMs 내에 maxCalls 개 이상의 호출이 있으면, 가장 오래된 호출이 윈도우를 벗어날 때까지 대기한다.
     */
    private static final class SlidingWindowRateLimiter {

        private final int maxCalls;
        private final long windowMs;
        private final Deque<Long> timestamps = new ArrayDeque<>();

        SlidingWindowRateLimiter(int maxCalls, long windowMs) {
            this.maxCalls = maxCalls;
            this.windowMs = windowMs;
        }

        synchronized void acquire() {
            while (true) {
                long now = System.currentTimeMillis();
                // 윈도우 밖으로 나간 타임스탬프 제거
                while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMs) {
                    timestamps.pollFirst();
                }
                if (timestamps.size() < maxCalls) {
                    timestamps.addLast(now);
                    return;
                }
                // 가장 오래된 호출이 윈도우를 벗어날 때까지 대기
                long waitMs = windowMs - (now - timestamps.peekFirst()) + 50;
                log.debug("[RateLimiter] slot full ({}/{}), waiting {}ms", timestamps.size(), maxCalls, waitMs);
                try {
                    wait(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LlmClientException("Interrupted while waiting for Gemini rate-limit slot", e);
                }
            }
        }
    }
}
