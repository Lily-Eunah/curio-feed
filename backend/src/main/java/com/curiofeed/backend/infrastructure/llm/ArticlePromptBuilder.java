package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.springframework.stereotype.Component;

@Component
public class ArticlePromptBuilder {

    private static final String TEMPLATE = """
            You are an English learning content creator.
            Rewrite the article below for a {LEVEL} level English learner.

            LEVEL definitions:
            - EASY: simple vocabulary, short sentences, A2-B1 level
            - MEDIUM: intermediate vocabulary, B1-B2 level
            - HARD: advanced vocabulary, complex sentences, C1 level

            Return ONLY raw JSON. DO NOT include:
            - markdown formatting (no ```json blocks)
            - explanations before or after the JSON
            - comments inside the JSON
            - any text outside the JSON object

            Output format:
            {
              "content": "rewritten article text",
              "vocabularies": [
                {"word": "word", "definition": "definition in English", "exampleSentence": "example sentence"}
              ],
              "quizzes": [
                {
                  "type": "MULTIPLE_CHOICE",
                  "question": "question text",
                  "options": {
                    "choices": [
                      {"key": "A", "text": "choice text", "explanation": "why this is wrong (only for wrong answers)"},
                      {"key": "B", "text": "choice text"},
                      {"key": "C", "text": "choice text"},
                      {"key": "D", "text": "choice text"}
                    ]
                  },
                  "correctAnswer": "A",
                  "explanation": "why the correct answer is correct"
                }
              ]
            }

            Include exactly 5 vocabularies and 3 quizzes (2 MULTIPLE_CHOICE, 1 SHORT_ANSWER).
            Limit total output to under 1500 tokens.

            SHORT_ANSWER quiz format:
            {
              "type": "SHORT_ANSWER",
              "question": "Fill in the blank: ___",
              "options": {},
              "correctAnswer": "single word or short phrase",
              "explanation": "explanation"
            }

            [ARTICLE]
            {ORIGINAL_CONTENT}
            """;

    public String build(String originalContent, DifficultyLevel level) {
        return TEMPLATE
                .replace("{LEVEL}", level.name())
                .replace("{ORIGINAL_CONTENT}", originalContent);
    }
}
