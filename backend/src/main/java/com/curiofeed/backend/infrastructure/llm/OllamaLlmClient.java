package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.OllamaProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OllamaLlmClient implements LlmClient {

    private final OllamaProperties properties;
    private final RestClient restClient;

    public OllamaLlmClient(OllamaProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public String generate(String prompt) {
        OllamaRequest request = new OllamaRequest(properties.model(), prompt, false);
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
