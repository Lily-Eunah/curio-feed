package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.OllamaProperties;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class OllamaLlmClient implements LlmClient {

    private final String model;
    private final RestClient restClient;

    public OllamaLlmClient(OllamaProperties properties, String model, RestClient.Builder restClientBuilder) {
        this.model = model;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public String generate(String prompt) {
        OllamaRequest request = new OllamaRequest(model, prompt, false);
        try {
            OllamaResponse response = restClient.post()
                    .uri("/api/generate")
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        throw new LlmClientException("LLM call failed: HTTP " + res.getStatusCode());
                    })
                    .body(OllamaResponse.class);

            if (response == null) {
                throw new LlmClientException("LLM call failed: empty response");
            }
            return response.response();
        } catch (LlmClientException e) {
            throw e;
        } catch (RestClientException e) {
            throw new LlmClientException("LLM call failed: " + e.getMessage(), e);
        }
    }
}
