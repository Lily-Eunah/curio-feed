package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.QuizAttemptRequest;
import com.curiofeed.backend.api.dto.QuizAttemptResponse;
import com.curiofeed.backend.domain.entity.Quiz;
import com.curiofeed.backend.domain.entity.QuizType;
import com.curiofeed.backend.domain.repository.QuizRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @InjectMocks
    private QuizServiceImpl quizService;

    @Test
    @DisplayName("퀴즈 제출 시, 리포지토리에서 퀴즈를 조회하고 채점 결과를 반환한다")
    void shouldEvaluateQuizAndReturnResult_whenValidRequest() {
        // given
        UUID quizId = UUID.randomUUID();
        Quiz mockQuiz = createMockQuiz(quizId, "Paris", "Paris is the capital of France.");
        
        QuizAttemptRequest request = QuizAttemptRequest.builder().answerText("Paris").build();

        given(quizRepository.findById(quizId)).willReturn(Optional.of(mockQuiz));

        // when
        QuizAttemptResponse response = quizService.attemptQuiz(quizId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isCorrect()).isTrue();
        assertThat(response.correctAnswer()).isEqualTo("Paris");
        assertThat(response.explanation()).isEqualTo("Paris is the capital of France.");
    }

    @Test
    @DisplayName("존재하지 않는 퀴즈 ID로 제출 요청 시 EntityNotFoundException을 발생시킨다")
    void shouldThrowEntityNotFoundException_whenQuizDoesNotExist() {
        // given
        UUID quizId = UUID.randomUUID();
        QuizAttemptRequest request = QuizAttemptRequest.builder().answerText("Paris").build();

        given(quizRepository.findById(quizId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizService.attemptQuiz(quizId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Quiz not found with id: " + quizId);
    }

    // --- Reflection Helper ---
    private Quiz createMockQuiz(UUID id, String correctAnswer, String explanation) {
        try {
            var constructor = Quiz.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Quiz quiz = constructor.newInstance();
            
            Field idField = Quiz.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(quiz, id);
            
            Field correctAnsField = Quiz.class.getDeclaredField("correctAnswer");
            correctAnsField.setAccessible(true);
            correctAnsField.set(quiz, correctAnswer);
            
            Field expField = Quiz.class.getDeclaredField("explanation");
            expField.setAccessible(true);
            expField.set(quiz, explanation);
            
            Field typeField = Quiz.class.getDeclaredField("type");
            typeField.setAccessible(true);
            typeField.set(quiz, QuizType.SHORT_ANSWER);
            
            return quiz;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
