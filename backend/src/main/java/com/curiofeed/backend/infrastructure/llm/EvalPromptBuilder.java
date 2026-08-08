package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EvalPromptBuilder {

    public String buildEvaluationPrompt(String originalArticle, String generatedContent, DifficultyLevel level) {
        return """
                You are an expert AI evaluator assessing simplified English news articles generated for language learners.

                EVALUATION RUBRIC:
                1. factual_accuracy (0.0 to 1.0): Check if the generated content accurately reflects facts from the original article without hallucinations, distortions, or unbacked claims.
                2. level_appropriateness (0.0 to 1.0): Check if vocabulary complexity and sentence length fit CEFR difficulty level '%s'.
                3. engagement (0.0 to 1.0): Check if the tone, flow, and narrative structure are engaging and natural for readers.
                4. safety (0.0 to 1.0): Check for harmful language, offensive bias, or severe copyright verbatim duplication.

                Original Article:
                %s

                Generated Article Content (%s level):
                %s

                Return ONLY valid JSON matching this schema:
                {
                  "factualAccuracy": 0.95,
                  "levelAppropriateness": 0.90,
                  "engagement": 0.85,
                  "safety": 1.0,
                  "overall": 0.925,
                  "explanation": "Brief explanation of scores..."
                }
                """.formatted(level.name(), originalArticle, level.name(), generatedContent);
    }

    public static Map<String, Object> evalSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("factualAccuracy", Map.of("type", "number"));
        properties.put("levelAppropriateness", Map.of("type", "number"));
        properties.put("engagement", Map.of("type", "number"));
        properties.put("safety", Map.of("type", "number"));
        properties.put("overall", Map.of("type", "number"));
        properties.put("explanation", Map.of("type", "string"));
        schema.put("properties", properties);
        schema.put("required", List.of("factualAccuracy", "levelAppropriateness", "engagement", "safety", "overall"));
        return schema;
    }
}
