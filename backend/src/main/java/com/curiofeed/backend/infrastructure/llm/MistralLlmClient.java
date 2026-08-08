package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.MistralProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MistralLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MistralLlmClient.class);
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    private static final int MAX_RATE_LIMIT_RETRIES = 2;
    private static final long BACKOFF_MS = 10_000L;

    private final String apiKey;
    private final String model;
    private final Double temperature;
    private final RestClient restClient;
    private final MeterRegistry meterRegistry;

    public MistralLlmClient(MistralProperties properties, String model, RestClient.Builder restClientBuilder) {
        this(properties, model, restClientBuilder, null);
    }

    public MistralLlmClient(MistralProperties properties, String model, RestClient.Builder restClientBuilder, MeterRegistry meterRegistry) {
        this.apiKey = properties.apiKey();
        this.model = model;
        this.temperature = properties.temperature();
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrlOrDefault()).build();
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String getModelName() {
        return this.model;
    }

    @Override
    public String generate(String prompt) {
        return generate(prompt, null);
    }

    @Override
    public String generate(String prompt, Map<String, Object> schema) {
        for (int attempt = 1; attempt <= MAX_RATE_LIMIT_RETRIES + 1; attempt++) {
            try {
                return doGenerate(prompt, schema);
            } catch (RateLimitException e) {
                recordRateLimitHit();
                if (attempt > MAX_RATE_LIMIT_RETRIES) {
                    throw new LlmClientException(
                            "Mistral rate limit exceeded after " + MAX_RATE_LIMIT_RETRIES + " retries (model=" + model + ")");
                }
                log.warn("[MistralLlmClient] 429 received — waiting {}s before retry {}/{} model={}",
                        BACKOFF_MS / 1000, attempt, MAX_RATE_LIMIT_RETRIES, model);
                sleepUninterruptibly(BACKOFF_MS);
            }
        }
        throw new LlmClientException("Mistral generate unreachable");
    }

    private void recordRateLimitHit() {
        if (meterRegistry != null) {
            Counter.builder("curiofeed.llm.mistral.ratelimit.hits")
                    .tag("model", model)
                    .register(meterRegistry)
                    .increment();
        }
    }

    private void recordRequestMetric(String status, long durationMs) {
        if (meterRegistry != null) {
            Counter.builder("curiofeed.llm.mistral.requests")
                    .tag("status", status)
                    .tag("model", model)
                    .register(meterRegistry)
                    .increment();
            Timer.builder("curiofeed.llm.mistral.duration")
                    .tag("status", status)
                    .tag("model", model)
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);
        }
    }

    private String doGenerate(String prompt, Map<String, Object> schema) {
        MistralRequest.ResponseFormat responseFormat = (schema != null)
                ? MistralRequest.ResponseFormat.jsonObject()
                : null;

        MistralRequest request = new MistralRequest(
                model,
                List.of(new MistralRequest.Message("user", prompt)),
                temperature,
                responseFormat
        );

        long startTimeMs = System.currentTimeMillis();
        try {
            MistralResponse response = restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + (apiKey != null ? apiKey : ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 429,
                            (req, res) -> {
                                String body = new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                                log.warn("[MistralLlmClient] HTTP 429 response body: {}", body);
                                throw new RateLimitException("HTTP 429: " + body);
                            })
                    .onStatus(status -> status.isError(),
                            (req, res) -> {
                                throw new LlmClientException("Mistral call failed: HTTP " + res.getStatusCode());
                            })
                    .body(MistralResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new LlmClientException("Mistral call failed: empty response");
            }

            MistralResponse.Choice choice = response.choices().get(0);
            if (choice.message() == null || choice.message().content() == null) {
                throw new LlmClientException("Mistral call failed: empty content");
            }

            recordRequestMetric("success", System.currentTimeMillis() - startTimeMs);
            return choice.message().content();

        } catch (RateLimitException e) {
            recordRequestMetric("rate_limited", System.currentTimeMillis() - startTimeMs);
            throw e;
        } catch (LlmClientException e) {
            recordRequestMetric("error", System.currentTimeMillis() - startTimeMs);
            throw e;
        } catch (RestClientException e) {
            recordRequestMetric("error", System.currentTimeMillis() - startTimeMs);
            throw new LlmClientException("Mistral call failed: " + e.getMessage(), e);
        }
    }

    private void sleepUninterruptibly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LlmClientException("Interrupted during Mistral rate-limit backoff", ie);
        }
    }

    private static class RateLimitException extends RuntimeException {
        RateLimitException(String message) { super(message); }
    }
}
