package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.OllamaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaLlmClientTest {

    private static final String BASE_URL = "http://localhost:11434";
    private static final String MODEL = "gemma3:4b";

    private MockRestServiceServer mockServer;
    private OllamaLlmClient client;

    @BeforeEach
    void setUp() {
        OllamaProperties properties = new OllamaProperties(BASE_URL, MODEL, 5, 120);
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new OllamaLlmClient(properties, builder);
    }

    @Test
    @DisplayName("정상 응답 시 'response' 필드 값을 반환한다")
    void generate_success() {
        String responseBody = """
                {
                  "model": "gemma3:4b",
                  "response": "This is the generated text.",
                  "done": true
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/api/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String result = client.generate("test prompt");

        assertThat(result).isEqualTo("This is the generated text.");
        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 5xx 응답 시 LlmClientException이 발생한다")
    void generate_serverError_throwsLlmClientException() {
        mockServer.expect(requestTo(BASE_URL + "/api/generate"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.generate("test prompt"))
                .isInstanceOf(LlmClientException.class)
                .hasMessageContaining("LLM call failed");
    }

    @Test
    @DisplayName("HTTP 4xx 응답 시 LlmClientException이 발생한다")
    void generate_clientError_throwsLlmClientException() {
        mockServer.expect(requestTo(BASE_URL + "/api/generate"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.generate("test prompt"))
                .isInstanceOf(LlmClientException.class)
                .hasMessageContaining("LLM call failed");
    }
}
