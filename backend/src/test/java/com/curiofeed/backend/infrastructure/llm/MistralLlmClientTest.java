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

    private static final String MODEL = "mistral-small-latest";
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
                API_KEY, MODEL, "ministral-8b-latest", "https://api.mistral.ai", 5, 5, 0.3
        );
        client = new MistralLlmClient(properties, MODEL, builder, meterRegistry);
    }

    @Test
    @DisplayName("getModelName returns configured model name")
    void getModelName_returnsConfiguredModel() {
        assertThat(client.getModelName()).isEqualTo(MODEL);
    }

    @Test
    @DisplayName("generate sends authorization header and returns message content")
    void generate_successResponse_returnsContent() {
        String jsonResponseBody = """
                {
                  "id": "chatcmpl-test",
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

        assertThat(meterRegistry.find("curiofeed.llm.mistral.requests").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("generate with schema includes response_format json_object")
    void generate_withSchema_includesResponseFormat() {
        String jsonResponseBody = """
                {
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
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess(jsonResponseBody, MediaType.APPLICATION_JSON));

        String result = client.generate("Generate json", Map.of("type", "object"));

        assertThat(result).contains("result");
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
