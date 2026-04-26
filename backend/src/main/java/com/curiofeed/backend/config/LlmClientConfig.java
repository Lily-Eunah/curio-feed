package com.curiofeed.backend.config;

import com.curiofeed.backend.infrastructure.llm.LlmClient;
import com.curiofeed.backend.infrastructure.llm.OllamaLlmClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class LlmClientConfig {

    @Bean
    @Primary
    public LlmClient primaryLlmClient(OllamaProperties properties) {
        return new OllamaLlmClient(properties, properties.model(), createBuilder(properties));
    }

    @Bean
    @Qualifier("fallbackLlmClient")
    public LlmClient fallbackLlmClient(OllamaProperties properties) {
        String fallbackModel = properties.fallbackModel() != null
                ? properties.fallbackModel()
                : properties.model();
        return new OllamaLlmClient(properties, fallbackModel, createBuilder(properties));
    }

    private RestClient.Builder createBuilder(OllamaProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }
}
