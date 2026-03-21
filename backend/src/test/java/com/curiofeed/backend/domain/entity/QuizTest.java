package com.curiofeed.backend.domain.entity;

import com.curiofeed.backend.domain.model.QuizChoice;
import com.curiofeed.backend.domain.model.QuizEvaluationResult;
import com.curiofeed.backend.domain.model.QuizOptions;
import com.curiofeed.backend.domain.model.QuizSubmission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizTest {

    @Test
    @DisplayName("객관식 단일 정답 제출 시 정답 여부와 해설을 반환한다 (대소문자 무시, 공백 제거)")
    void evaluate_shouldReturnCorrectResult_ForSingleAnswer() {
        // given
        QuizOptions options = new QuizOptions(
                List.of(
                        new QuizChoice("A", "London", "London is the capital of the UK."),
                        new QuizChoice("B", "Paris", null),
                        new QuizChoice("C", "Berlin", "Berlin is the capital of Germany.")
                ),
                null
        );

        Quiz quiz = createQuiz(
                QuizType.MULTIPLE_CHOICE,
                "What is the capital of France?",
                "B",
                "Paris is the capital and most populous city of France.",
                options
        );

        // when (Correct answer with weird casing and spaces)
        QuizSubmission correctSub = QuizSubmission.builder().choiceId("  b ").build();
        QuizEvaluationResult correctResponse = quiz.evaluate(correctSub);

        // then
        assertThat(correctResponse.isCorrect()).isTrue();
        assertThat(correctResponse.correctAnswer()).isEqualTo("B");
        assertThat(correctResponse.explanation()).isEqualTo("Paris is the capital and most populous city of France.");

        // when (Wrong answer - should use specific choice's fallback explanation if exists)
        QuizSubmission wrongSub = QuizSubmission.builder().choiceId("A").build();
        QuizEvaluationResult wrongResponse = quiz.evaluate(wrongSub);

        // then
        assertThat(wrongResponse.isCorrect()).isFalse();
        assertThat(wrongResponse.correctAnswer()).isEqualTo("B");
        // "A" option has a specific explanation
        assertThat(wrongResponse.explanation()).isEqualTo("London is the capital of the UK.");

        // when (Wrong answer with no specific explanation - should fallback to quiz explanation)
        QuizSubmission fallbackSub = QuizSubmission.builder().choiceId("D").build();
        QuizEvaluationResult fallbackResponse = quiz.evaluate(fallbackSub);

        // then
        assertThat(fallbackResponse.isCorrect()).isFalse();
        assertThat(fallbackResponse.correctAnswer()).isEqualTo("B");
        assertThat(fallbackResponse.explanation()).isEqualTo("Paris is the capital and most populous city of France.");
    }

    @Test
    @DisplayName("주관식 형태의 정답 제출 시 정규화를 통해 정답 여부를 확인한다 (공백 및 특수문자 무시)")
    void evaluate_shouldReturnCorrectResult_ForShortAnswer() {
        Quiz quiz = createQuiz(
                QuizType.SHORT_ANSWER,
                "What is a database index?",
                "Database Index",
                "An index speeds up data retrieval.",
                null
        );

        QuizSubmission correctSub1 = QuizSubmission.builder().answerText("  database    index ").build();
        assertThat(quiz.evaluate(correctSub1).isCorrect()).isTrue();
        
        QuizSubmission correctSub2 = QuizSubmission.builder().answerText("Database Index.").build();
        assertThat(quiz.evaluate(correctSub2).isCorrect()).isTrue();
        
        QuizSubmission correctSub3 = QuizSubmission.builder().answerText("database index!  ").build();
        assertThat(quiz.evaluate(correctSub3).isCorrect()).isTrue();
    }

    @Test
    @DisplayName("배열 형태의 정답 제출 시 정확히 일치하는지 확인한다 (SCRAMBLE)")
    void evaluate_shouldReturnCorrectResult_ForArrayAnswer() {
        // given
        Quiz quiz = createQuiz(
                QuizType.SCRAMBLE,
                "Order the words",
                "I live in London", 
                "주어 + 동사 + 전치사구 순서로 구성됩니다.",
                null
        );

        // when (Correct array)
        QuizSubmission correctSub = QuizSubmission.builder()
                .answerList(List.of("I", "live", "in", "London"))
                .build();
        QuizEvaluationResult correctResponse = quiz.evaluate(correctSub);

        // then
        assertThat(correctResponse.isCorrect()).isTrue();
        assertThat(correctResponse.correctAnswer()).isEqualTo(List.of("I", "live", "in", "London"));

        // when (Wrong array)
        QuizSubmission wrongSub = QuizSubmission.builder()
                .answerList(List.of("I", "in", "live", "London"))
                .build();
        QuizEvaluationResult wrongResponse = quiz.evaluate(wrongSub);

        // then
        assertThat(wrongResponse.isCorrect()).isFalse();
        assertThat(wrongResponse.explanation()).isEqualTo("주어 + 동사 + 전치사구 순서로 구성됩니다.");
    }

    // --- Reflection Helpers ---
    private Quiz createQuiz(QuizType type, String question, String correctAnswer, String explanation, QuizOptions options) {
        try {
            var constructor = Quiz.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Quiz quiz = constructor.newInstance();
            setField(quiz, "type", type);
            setField(quiz, "question", question);
            setField(quiz, "correctAnswer", correctAnswer);
            setField(quiz, "explanation", explanation);
            setField(quiz, "options", options);
            return quiz;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = Quiz.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
