package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.MistralProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MistralLlmClientTest {

    private static final String MODEL = "mistral-small-2501";
    private static final String API_KEY = "test-mistral-api-key";

    private MockRestServiceServer mockServer;
    private MistralLlmClient client;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        meterRegistry = new SimpleMeterRegistry();
        MistralProperties properties = new MistralProperties(
                API_KEY, MODEL, "ministral-8b-2410", "https://api.mistral.ai", 5, 5, 0.3
        );
        client = new MistralLlmClient(properties, MODEL, builder, meterRegistry);
    }

    @Test
    @DisplayName("getModelName returns configured model name")
    void getModelName_returnsConfiguredModel() {
        assertThat(client.getModelName()).isEqualTo(MODEL);
    }

    @Test
    @DisplayName("generate sends authorization header and returns message content with resolved model metric tag")
    void generate_successResponse_returnsContentAndRecordsResolvedModel() {
        String jsonResponseBody = """
                {
                  "id": "chatcmpl-test",
                  "model": "mistral-small-2501",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Hello world from Mistral"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 5,
                    "total_tokens": 15
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.mistral.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(jsonResponseBody, MediaType.APPLICATION_JSON));

        String result = client.generate("Say hello");

        assertThat(result).isEqualTo("Hello world from Mistral");
        mockServer.verify();

        assertThat(meterRegistry.find("curiofeed.llm.mistral.requests")
                .tag("model", "mistral-small-2501")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("generate with schema sends strict json_schema response_format")
    void generate_withSchema_includesStrictJsonSchema() {
        String jsonResponseBody = """
                {
                  "model": "mistral-small-2501",
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"result\\": \\"success\\"}"
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://api.mistral.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andRespond(withSuccess(jsonResponseBody, MediaType.APPLICATION_JSON));

        String result = client.generate("Generate json", Map.of("type", "object"));

        assertThat(result).contains("result");
        mockServer.verify();
    }

    @Test
    @DisplayName("listModels sends GET request to /v1/models")
    void listModels_success() {
        String modelsResponseBody = """
                {
                  "object": "list",
                  "data": [
                    { "id": "mistral-small-2501", "object": "model" },
                    { "id": "ministral-8b-2410", "object": "model" }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://api.mistral.ai/v1/models"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andRespond(withSuccess(modelsResponseBody, MediaType.APPLICATION_JSON));

        String modelsJson = client.listModels();

        assertThat(modelsJson).contains("mistral-small-2501", "ministral-8b-2410");
        mockServer.verify();
    }

    @Test
    @DisplayName("generate handles HTTP errors appropriately")
    void generate_httpError_throwsLlmClientException() {
        mockServer.expect(requestTo("https://api.mistral.ai/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.generate("Error test"))
                .isInstanceOf(LlmClientException.class)
                .hasMessageContaining("Mistral call failed: HTTP 400");
    }
}
