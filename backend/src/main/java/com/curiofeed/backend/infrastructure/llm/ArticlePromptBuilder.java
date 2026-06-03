package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.springframework.stereotype.Component;

@Component
public class ArticlePromptBuilder {

    private static final String SOURCE_DIGEST_TEMPLATE = """
            You are a professional editor.
            Summarize the core facts of the following article in a structured format.
            Extract the central story and core facts without adding or interpreting information.
            Ensure sufficient factual density for a 260~420 word summary.
            Do not use a narrative format.

            Output strictly in this JSON format:
            {
              "sourceDigest": {
                "centralStory": "1-2 sentence summary of the core event",
                "coreFacts": ["fact 1", "fact 2", ...],
                "supportingDetails": ["detail 1", "detail 2", ...],
                "omittedDetails": ["list of omitted elements"]
              }
            }

            [ARTICLE]
            {ORIGINAL_CONTENT}
            """;

    private static final String CONTENT_TEMPLATE = """
            You are an English learning content creator.
            Rewrite the given source text for a {LEVEL} level English learner.

            [LEVEL REQUIREMENTS]
            - EASY (A2-B1): 12~15 words per sentence. 10yo native level vocab. 5~6 core facts. 180~260 words (max 320). Exactly 3 paragraphs (event/details/background).
            - MEDIUM (B1-B2): Varied sentence lengths. News journalism vocab. 6~8 core facts. 220~320 words (max 380). 3~4 paragraphs.
            - HARD (C1): Complex structure. Advanced/professional vocab. 8~10 core facts. 280~420 words (max 500). 3~4 paragraphs.

            [CONSTRAINTS]
            - Do not add information not present in the source.
            - Use natural prose only. Do not use bullets, headers, or lists.
            - Separate paragraphs with exactly \\n\\n. (Ensure it is escaped in JSON)
            - Word count limits are strictly prioritized over content preservation.

            Output strictly in this JSON format:
            {
              "content": "paragraph 1\\n\\nparagraph 2\\n\\nparagraph 3"
            }

            [SOURCE TEXT]
            {SOURCE_TEXT}
            """;

    private static final String VOCABULARY_TEMPLATE = """
            You are an English learning content creator.
            Extract exactly 5 vocabulary words from the article provided below.

            [RULES]
            1. BASE FORM RULE (CRITICAL):
               - Submit the dictionary base form (e.g., 'targeted' -> 'target', 'restricting' -> 'restrict').
               - The word or its inflected form MUST appear in the article text.
            2. FORBIDDEN LIST: Do not use A1 level, overly simple words, or generic news words that appear everywhere.
            3. DEFINITION FORMAT: "[short meaning] — used when [specific situation or condition]"
               Example: "to prevent access to an area — used when a military power wants to cut off a region"
            4. EXAMPLE SENTENCE: Use a completely different context from the article (e.g., cooking, sports, school, shopping). Do NOT reuse article content.

            Output strictly in this JSON format:
            {
              "vocabularies": [
                {
                  "word": "base_form_word",
                  "definition": "...",
                  "exampleSentence": "..."
                }
              ]
            }

            [ARTICLE CONTENT]
            {CONTENT}
            """;

    private static final String QUIZ_TEMPLATE = """
            You are an English learning content creator.
            Create exactly 3 quizzes based on the article and vocabulary provided below.

            [QUIZ RULES]
            Q1 - MULTIPLE_CHOICE (Passage Comprehension):
            - Test core theme or overall situation.
            - Do NOT ask about specific numbers, percentages, dates, or country names.
            - Do NOT copy sentences to ask who/what/when.
            - Format: 4 choices, 1 correct answer, plausible distractors.

            Q2 - MULTIPLE_CHOICE (Passage Reasoning):
            - Test cause/effect, motivation, or implications.
            - Do NOT ask vocabulary definitions or "Which sentence uses X correctly?".
            - Format: 4 choices, requires full passage understanding.

            Q3 - SHORT_ANSWER (Vocabulary in Context):
            - Test article comprehension AND vocabulary usage simultaneously.
            - Process:
              1. Choose 1 word from the vocabulary list as TARGET_WORD.
              2. Ask a question about the article that REQUIRES using TARGET_WORD in the answer.
              3. Question Format: "In one sentence, [article-based task]. Use the word '[TARGET_WORD]' in your answer."
              4. correctAnswer: A complete model sentence containing TARGET_WORD (or inflected form).
              5. explanation: "Target word: TARGET_WORD"
              6. options MUST be an empty object {}.
            - Do NOT use generic fill-in-the-blanks or "Use the word in any sentence".

            Output strictly in this JSON format:
            {
              "quizzes": [
                {
                  "type": "MULTIPLE_CHOICE",
                  "question": "...",
                  "options": {
                    "choices": [
                      {"key": "A", "text": "...", "explanation": "..."},
                      {"key": "B", "text": "...", "explanation": "..."},
                      {"key": "C", "text": "...", "explanation": "..."},
                      {"key": "D", "text": "...", "explanation": "..."}
                    ]
                  },
                  "correctAnswer": "A",
                  "explanation": "..."
                },
                {
                  "type": "MULTIPLE_CHOICE",
                  "question": "...",
                  "options": {
                    "choices": [
                      {"key": "A", "text": "...", "explanation": "..."},
                      {"key": "B", "text": "...", "explanation": "..."},
                      {"key": "C", "text": "...", "explanation": "..."},
                      {"key": "D", "text": "...", "explanation": "..."}
                    ]
                  },
                  "correctAnswer": "B",
                  "explanation": "..."
                },
                {
                  "type": "SHORT_ANSWER",
                  "question": "In one sentence, [task]. Use the word '[TARGET_WORD]' in your answer.",
                  "options": {},
                  "correctAnswer": "...",
                  "explanation": "Target word: [TARGET_WORD]"
                }
              ]
            }

            [VOCABULARY]
            {VOCABULARY}

            [ARTICLE CONTENT]
            {CONTENT}
            """;

    public String buildSourceDigestPrompt(String originalContent) {
        return SOURCE_DIGEST_TEMPLATE.replace("{ORIGINAL_CONTENT}", originalContent);
    }

    public String buildContentPrompt(String sourceText, DifficultyLevel level) {
        return CONTENT_TEMPLATE
                .replace("{LEVEL}", level.name())
                .replace("{SOURCE_TEXT}", sourceText);
    }

    public String buildVocabularyPrompt(String content, DifficultyLevel level) {
        return VOCABULARY_TEMPLATE.replace("{CONTENT}", content);
    }

    public String buildQuizPrompt(String content, String vocabularyJson) {
        return QUIZ_TEMPLATE
                .replace("{CONTENT}", content)
                .replace("{VOCABULARY}", vocabularyJson);
    }
}
