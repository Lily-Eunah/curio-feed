package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.GeminiProperties;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeminiLlmClient implements LlmClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GENERATE_PATH = "/v1beta/models/{model}:generateContent";

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
        GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig(
                temperature,
                schema != null ? "application/json" : "text/plain",
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
                    .onStatus(status -> status.isError(), (req, res) -> {
                        throw new LlmClientException("Gemini call failed: HTTP " + res.getStatusCode());
                    })
                    .body(GeminiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new LlmClientException("Gemini call failed: empty response");
            }

            List<GeminiResponse.Part> parts = response.candidates().get(0).content().parts();
            if (parts == null || parts.isEmpty()) {
                throw new LlmClientException("Gemini call failed: no content parts");
            }
            return parts.get(0).text();
        } catch (LlmClientException e) {
            throw e;
        } catch (RestClientException e) {
            throw new LlmClientException("Gemini call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gemini's responseSchema does not support `additionalProperties`.
     * Strip it recursively so the API accepts the schema.
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
}
