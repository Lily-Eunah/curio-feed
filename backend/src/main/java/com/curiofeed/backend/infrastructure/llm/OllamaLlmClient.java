package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.OllamaProperties;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OllamaLlmClient implements LlmClient {

    private static final Map<String, Object> GENERATION_SCHEMA = buildGenerationSchema();

    private final String model;
    private final int numCtx;
    private final Double temperature;
    private final RestClient restClient;

    public OllamaLlmClient(OllamaProperties properties, String model, RestClient.Builder restClientBuilder) {
        this(properties, model, properties.temperature(), restClientBuilder);
    }

    public OllamaLlmClient(OllamaProperties properties, String model, Double temperature, RestClient.Builder restClientBuilder) {
        this.model = model;
        this.numCtx = properties.numCtx();
        this.temperature = temperature;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public String generate(String prompt) {
        return generate(prompt, GENERATION_SCHEMA);
    }

    @Override
    public String generate(String prompt, Map<String, Object> schema) {
        OllamaRequest request = new OllamaRequest(model, prompt, false, new OllamaRequest.Options(numCtx, temperature), schema);
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

    private static Map<String, Object> buildGenerationSchema() {
        Map<String, Object> vocabItemProps = new LinkedHashMap<>();
        vocabItemProps.put("word", Map.of("type", "string"));
        vocabItemProps.put("definition", Map.of("type", "string"));
        vocabItemProps.put("exampleSentence", Map.of("type", "string"));

        Map<String, Object> vocabItem = new LinkedHashMap<>();
        vocabItem.put("type", "object");
        vocabItem.put("required", List.of("word", "definition", "exampleSentence"));
        vocabItem.put("properties", vocabItemProps);
        vocabItem.put("additionalProperties", false);

        Map<String, Object> vocabularies = new LinkedHashMap<>();
        vocabularies.put("type", "array");
        vocabularies.put("minItems", 5);
        vocabularies.put("maxItems", 5);
        vocabularies.put("items", vocabItem);

        Map<String, Object> quizItemProps = new LinkedHashMap<>();
        quizItemProps.put("type", Map.of("type", "string", "enum", List.of("MULTIPLE_CHOICE", "SHORT_ANSWER")));
        quizItemProps.put("question", Map.of("type", "string"));
        quizItemProps.put("options", Map.of("type", "object"));
        quizItemProps.put("correctAnswer", Map.of("type", "string"));
        quizItemProps.put("explanation", Map.of("type", "string"));

        Map<String, Object> quizItem = new LinkedHashMap<>();
        quizItem.put("type", "object");
        quizItem.put("required", List.of("type", "question", "options", "correctAnswer", "explanation"));
        quizItem.put("properties", quizItemProps);
        quizItem.put("additionalProperties", false);

        Map<String, Object> quizzes = new LinkedHashMap<>();
        quizzes.put("type", "array");
        quizzes.put("minItems", 3);
        quizzes.put("maxItems", 3);
        quizzes.put("items", quizItem);

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("content", Map.of("type", "string"));
        rootProps.put("vocabularies", vocabularies);
        rootProps.put("quizzes", quizzes);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("content", "vocabularies", "quizzes"));
        schema.put("properties", rootProps);
        schema.put("additionalProperties", false);

        return Map.copyOf(schema);
    }
}
