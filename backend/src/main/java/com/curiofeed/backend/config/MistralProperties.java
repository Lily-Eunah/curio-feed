package com.curiofeed.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.mistral")
public record MistralProperties(
        String apiKey,
        String model,
        String fallbackModel,
        String baseUrl,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        Double temperature
) {
    public String getBaseUrlOrDefault() {
        return (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "https://api.mistral.ai";
    }
}
