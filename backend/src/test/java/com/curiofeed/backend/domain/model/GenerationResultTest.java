package com.curiofeed.backend.domain.model;

import com.curiofeed.backend.domain.entity.QuizType;
import com.curiofeed.backend.infrastructure.llm.DefaultLlmResponseParser;
import com.curiofeed.backend.infrastructure.llm.LlmResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationResultTest {

    private LlmResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new DefaultLlmResponseParser(new ObjectMapper());
    }

    private static final String FULL_JSON = """
            {
              "content": "Rewritten article content.",
              "vocabularies": [
                {"word": "apple", "definition": "a fruit", "exampleSentence": "I ate an apple."}
              ],
              "quizzes": [
                {
                  "type": "MULTIPLE_CHOICE",
                  "question": "What is an apple?",
                  "options": {"choices": [{"key": "A", "text": "a fruit"}, {"key": "B", "text": "a car"}]},
                  "correctAnswer": "A",
                  "explanation": "Apple is a fruit."
                }
              ]
            }
            """;

    @Test
    @DisplayName("전체 JSON을 GenerationResult로 파싱한다")
    void parse_fullJson_success() {
        GenerationResult result = parser.parse(FULL_JSON, GenerationResult.class);

        assertThat(result.content()).isEqualTo("Rewritten article content.");
        assertThat(result.vocabularies()).hasSize(1);
        assertThat(result.vocabularies().get(0).word()).isEqualTo("apple");
        assertThat(result.quizzes()).hasSize(1);
        assertThat(result.quizzes().get(0).type()).isEqualTo(QuizType.MULTIPLE_CHOICE);
    }

    @Test
    @DisplayName("hasContent: content가 있으면 true")
    void hasContent_nonBlank_returnsTrue() {
        GenerationResult result = new GenerationResult("some content", List.of(), List.of());
        assertThat(result.hasContent()).isTrue();
    }

    @Test
    @DisplayName("hasContent: content가 null이면 false")
    void hasContent_null_returnsFalse() {
        GenerationResult result = new GenerationResult(null, List.of(), List.of());
        assertThat(result.hasContent()).isFalse();
    }

    @Test
    @DisplayName("hasContent: content가 blank이면 false")
    void hasContent_blank_returnsFalse() {
        GenerationResult result = new GenerationResult("   ", List.of(), List.of());
        assertThat(result.hasContent()).isFalse();
    }

    @Test
    @DisplayName("hasVocabularies: 비어있지 않은 리스트면 true")
    void hasVocabularies_nonEmpty_returnsTrue() {
        var vocab = new GenerationResult.VocabularyData("word", "def", "example");
        GenerationResult result = new GenerationResult("content", List.of(vocab), List.of());
        assertThat(result.hasVocabularies()).isTrue();
    }

    @Test
    @DisplayName("hasVocabularies: null이면 false")
    void hasVocabularies_null_returnsFalse() {
        GenerationResult result = new GenerationResult("content", null, List.of());
        assertThat(result.hasVocabularies()).isFalse();
    }

    @Test
    @DisplayName("hasVocabularies: 빈 리스트면 false")
    void hasVocabularies_empty_returnsFalse() {
        GenerationResult result = new GenerationResult("content", List.of(), List.of());
        assertThat(result.hasVocabularies()).isFalse();
    }

    @Test
    @DisplayName("hasQuizzes: 비어있지 않은 리스트면 true")
    void hasQuizzes_nonEmpty_returnsTrue() {
        var quiz = new GenerationResult.QuizData(QuizType.SHORT_ANSWER, "q?", new QuizOptions(null, null), "ans", "exp");
        GenerationResult result = new GenerationResult("content", List.of(), List.of(quiz));
        assertThat(result.hasQuizzes()).isTrue();
    }

    @Test
    @DisplayName("hasQuizzes: null이면 false")
    void hasQuizzes_null_returnsFalse() {
        GenerationResult result = new GenerationResult("content", List.of(), null);
        assertThat(result.hasQuizzes()).isFalse();
    }

    @Test
    @DisplayName("vocabularies가 null인 JSON 파싱 후 hasVocabularies() false")
    void parse_nullVocabularies_hasVocabulariesFalse() {
        String json = """
                {"content": "text", "vocabularies": null, "quizzes": null}
                """;
        GenerationResult result = parser.parse(json, GenerationResult.class);
        assertThat(result.hasVocabularies()).isFalse();
        assertThat(result.hasQuizzes()).isFalse();
    }
}
