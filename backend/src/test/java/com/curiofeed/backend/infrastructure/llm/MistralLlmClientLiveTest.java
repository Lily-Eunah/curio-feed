package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.MistralProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live Mistral API Integration Test — Executes live calls to api.mistral.ai.
 */
@Tag("mistral")
class MistralLlmClientLiveTest {

    private static final Logger log = LoggerFactory.getLogger(MistralLlmClientLiveTest.class);

    private static final String BASE_URL = "https://api.mistral.ai";
    private static final String MODEL = System.getenv("MISTRAL_MODEL") != null
            ? System.getenv("MISTRAL_MODEL")
            : "mistral-small-2603";

    private MistralLlmClient client;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("MISTRAL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "REDACTED_MISTRAL_API_KEY";
        }
        MistralProperties properties = new MistralProperties(apiKey, MODEL, "ministral-8b-2410", BASE_URL, 10, 120, 0.3);
        client = new MistralLlmClient(properties, MODEL, RestClient.builder());
    }

    @Test
    @DisplayName("GET /v1/models returns active model list with version IDs")
    void listModels_realServer_returnsModelList() {
        String modelsJson = client.listModels();
        System.out.println("\n=================================================");
        System.out.println(" [MISTRAL LIVE API MODEL LIST]");
        System.out.println("-------------------------------------------------");
        System.out.println(modelsJson);
        System.out.println("=================================================\n");

        assertThat(modelsJson).isNotBlank();
        assertThat(modelsJson).contains("data");
    }

    @Test
    @DisplayName("Live generation with mistral-small-2603 returns non-empty text")
    void generate_realServer_returnsText() {
        String response = client.generate("Respond with 'OK' and one short sentence about English learning.");
        System.out.println("\n=================================================");
        System.out.println(" [MISTRAL LIVE API GENERATION RESPONSE]");
        System.out.println("-------------------------------------------------");
        System.out.println(response);
        System.out.println("=================================================\n");

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
        System.out.println("\n=================================================");
        System.out.println(" [MISTRAL LIVE API STRICT JSON_SCHEMA RESPONSE]");
        System.out.println("-------------------------------------------------");
        System.out.println(jsonResponse);
        System.out.println("=================================================\n");

        assertThat(jsonResponse).isNotBlank();
        assertThat(jsonResponse).contains("status");
    }
}
