package com.curiofeed.backend.domain.entity;

import com.curiofeed.backend.api.dto.QuizAttemptResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuizTest {

    @Test
    @DisplayName("객관식 단일 정답 제출 시 정답 여부와 해설을 반환한다 (대소문자 무시, 공백 제거)")
    void evaluate_shouldReturnCorrectResult_ForSingleAnswer() {
        // given
        Quiz quiz = createQuiz(
                QuizType.MULTIPLE_CHOICE,
                "What is the capital of France?",
                "Paris",
                "Paris is the capital and most populous city of France.",
                Map.of(
                        "A", "London",
                        "B", "Paris",
                        "C", "Berlin"
                ),
                Map.of(
                        "London", "London is the capital of the UK.",
                        "Berlin", "Berlin is the capital of Germany."
                )
        );

        // when (Correct answer with weird casing and spaces)
        QuizAttemptResponse correctResponse = quiz.evaluate("  pArIs ");

        // then
        assertThat(correctResponse.isCorrect()).isTrue();
        assertThat(correctResponse.correctAnswer()).isEqualTo("Paris");
        assertThat(correctResponse.explanation()).isEqualTo("Paris is the capital and most populous city of France.");

        // when (Wrong answer - should use specific choice's fallback explanation if exists)
        QuizAttemptResponse wrongResponse = quiz.evaluate("London");

        // then
        assertThat(wrongResponse.isCorrect()).isFalse();
        assertThat(wrongResponse.correctAnswer()).isEqualTo("Paris");
        // "London" option has a specific explanation
        assertThat(wrongResponse.explanation()).isEqualTo("London is the capital of the UK.");

        // when (Wrong answer with no specific explanation - should fallback to quiz explanation)
        QuizAttemptResponse fallbackResponse = quiz.evaluate("Madrid");

        // then
        assertThat(fallbackResponse.isCorrect()).isFalse();
        assertThat(fallbackResponse.correctAnswer()).isEqualTo("Paris");
        assertThat(fallbackResponse.explanation()).isEqualTo("Paris is the capital and most populous city of France.");
    }

    @Test
    @DisplayName("배열 형태의 정답 제출 시 정확히 일치하는지 확인한다 (SCRAMBLE)")
    void evaluate_shouldReturnCorrectResult_ForArrayAnswer() {
        // given
        // correctAnswer in JSON can be a List. We'll set it as a JSON string for now or change the type to Object.
        Quiz quiz = createScrambleQuiz(
                "I live in London", // In actual implementation it might be stored as JSON List or joined string. Let's assume generic Object evaluation.
                "주어 + 동사 + 전치사구 순서로 구성됩니다."
        );

        // when (Correct array)
        QuizAttemptResponse correctResponse = quiz.evaluate(List.of("I", "live", "in", "London"));

        // then
        assertThat(correctResponse.isCorrect()).isTrue();
        assertThat(correctResponse.correctAnswer()).isEqualTo(List.of("I", "live", "in", "London"));

        // when (Wrong array)
        QuizAttemptResponse wrongResponse = quiz.evaluate(List.of("I", "in", "live", "London"));

        // then
        assertThat(wrongResponse.isCorrect()).isFalse();
        assertThat(wrongResponse.explanation()).isEqualTo("주어 + 동사 + 전치사구 순서로 구성됩니다.");
    }

    // --- Reflection Helpers ---
    private Quiz createQuiz(QuizType type, String question, String correctAnswer, String explanation, Map<String, Object> options, Map<String, String> choiceExplanations) {
        try {
            var constructor = Quiz.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Quiz quiz = constructor.newInstance();
            setField(quiz, "type", type);
            setField(quiz, "question", question);
            setField(quiz, "correctAnswer", correctAnswer);
            setField(quiz, "explanation", explanation);
            setField(quiz, "options", options);
            // We simulate adding choice specific explanations to the map or entity.
            // Since Quiz entity doesn't have it natively yet, we will add it to the 'options' map as `choiceExplanations` for the evaluate logic to parse.
            if (options != null && choiceExplanations != null) {
               options = new java.util.HashMap<>(options);
               options.put("explanations", choiceExplanations);
               setField(quiz, "options", options);
            }
            return quiz;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Quiz createScrambleQuiz(Object correctAnswer, String explanation) {
         try {
             var constructor = Quiz.class.getDeclaredConstructor();
             constructor.setAccessible(true);
             Quiz quiz = constructor.newInstance();
             // Just a mock type for string array
             setField(quiz, "type", QuizType.SHORT_ANSWER); // Actually we might need SCRAMBLE later
             setField(quiz, "correctAnswer", correctAnswer);
             setField(quiz, "explanation", explanation);
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
