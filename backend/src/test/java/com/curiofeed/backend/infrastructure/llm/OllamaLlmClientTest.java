package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.OllamaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaLlmClientTest {

    private static final String BASE_URL = "http://192.168.45.100:11434";
    private static final String MODEL = "gemma4:e4b";
    //private static final String MODEL = "qwen3:14b-q4_K_M";

    private OllamaLlmClient client;

    @BeforeEach
    void setUp() {
        OllamaProperties properties = new OllamaProperties(BASE_URL, MODEL, null, 5, 120);
        client = new OllamaLlmClient(properties, MODEL, RestClient.builder());
    }

    @Test
    @DisplayName("실제 Ollama 서버에서 정상 응답을 받아온다")
    void generate_realServer_returnsNonEmptyResponse() {
        String result = client.generate("Say hello in one sentence.");

        assertThat(result).isNotBlank();
    }
}
