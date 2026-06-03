package com.curiofeed.backend.config;

import com.curiofeed.backend.infrastructure.llm.GeminiLlmClient;
import com.curiofeed.backend.infrastructure.llm.LlmClient;
import com.curiofeed.backend.infrastructure.llm.OllamaLlmClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class LlmClientConfig {

    // ── Gemini beans (active when ai.provider=gemini) ──────────────────────

    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
    public LlmClient primaryGeminiClient(GeminiProperties p) {
        return new GeminiLlmClient(p, p.model(), geminiBuilder(p));
    }

    @Bean
    @Qualifier("fallbackLlmClient")
    @ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
    public LlmClient fallbackGeminiClient(GeminiProperties p) {
        String fallback = p.fallbackModel() != null ? p.fallbackModel() : p.model();
        return new GeminiLlmClient(p, fallback, geminiBuilder(p));
    }

    // ── Ollama beans (active when ai.provider=ollama or not set) ───────────

    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
    public LlmClient primaryOllamaClient(OllamaProperties p) {
        return new OllamaLlmClient(p, p.model(), ollamaBuilder(p));
    }

    @Bean
    @Qualifier("fallbackLlmClient")
    @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
    public LlmClient fallbackOllamaClient(OllamaProperties p) {
        String fallback = p.fallbackModel() != null ? p.fallbackModel() : p.model();
        return new OllamaLlmClient(p, fallback, ollamaBuilder(p));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private RestClient.Builder geminiBuilder(GeminiProperties p) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(p.connectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(p.readTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    private RestClient.Builder ollamaBuilder(OllamaProperties p) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(p.connectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(p.readTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }
}
