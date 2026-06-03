package com.curiofeed.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String fallbackModel,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        Double temperature
) {
}
