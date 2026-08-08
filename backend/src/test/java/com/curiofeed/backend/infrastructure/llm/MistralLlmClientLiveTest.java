package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.MistralProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live Mistral API Integration Test — Disabled by default unless RUN_MISTRAL_TESTS=true.
 *
 * Execution:
 *   RUN_MISTRAL_TESTS=true MISTRAL_API_KEY=<key> ./gradlew test --tests "*MistralLlmClientLiveTest"
 */
@Tag("mistral")
@EnabledIfEnvironmentVariable(named = "RUN_MISTRAL_TESTS", matches = "true")
class MistralLlmClientLiveTest {

    private static final Logger log = LoggerFactory.getLogger(MistralLlmClientLiveTest.class);

    private static final String BASE_URL = "https://api.mistral.ai";
    private static final String MODEL = System.getenv("MISTRAL_MODEL") != null
            ? System.getenv("MISTRAL_MODEL")
            : "mistral-small-2501";

    private MistralLlmClient client;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("MISTRAL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MISTRAL_API_KEY environment variable is required for live tests");
        }
        MistralProperties properties = new MistralProperties(apiKey, MODEL, "ministral-8b-2410", BASE_URL, 10, 120, 0.3);
        client = new MistralLlmClient(properties, MODEL, RestClient.builder());
    }

    @Test
    @DisplayName("GET /v1/models returns active model list with version IDs")
    void listModels_realServer_returnsModelList() {
        String modelsJson = client.listModels();
        log.info("[MistralLiveTest] Available models:\n{}", modelsJson);

        assertThat(modelsJson).isNotBlank();
        assertThat(modelsJson).contains("data");
    }

    @Test
    @DisplayName("Live generation returns non-empty text")
    void generate_realServer_returnsText() {
        String response = client.generate("Respond with 'OK' and one short sentence about English learning.");
        log.info("[MistralLiveTest] Live response:\n{}", response);

        assertThat(response).isNotBlank();
    }

    @Test
    @DisplayName("Live generation with strict json_schema returns valid JSON")
    void generate_withSchema_realServer_returnsJson() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "status", Map.of("type", "string"),
                        "message", Map.of("type", "string")
                ),
                "required", List.of("status", "message"),
                "additionalProperties", false
        );

        String jsonResponse = client.generate("Generate JSON with status 'SUCCESS' and a brief greeting.", schema);
        log.info("[MistralLiveTest] Live JSON response:\n{}", jsonResponse);

        assertThat(jsonResponse).isNotBlank();
        assertThat(jsonResponse).contains("status");
    }
}
