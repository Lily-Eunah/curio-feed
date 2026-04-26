package com.curiofeed.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.ollama")
public record OllamaProperties(
        String baseUrl,
        String model,
        String fallbackModel,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
}
