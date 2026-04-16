package com.curiofeed.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class LlmClientConfig {

    @Bean
    public RestClient.Builder ollamaRestClientBuilder(OllamaProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }
}
