package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.QuizAttemptRequest;
import com.curiofeed.backend.api.dto.QuizAttemptResponse;
import com.curiofeed.backend.domain.entity.Article;
import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.Quiz;
import com.curiofeed.backend.domain.model.QuizEvaluationResult;
import com.curiofeed.backend.domain.model.QuizSubmission;
import com.curiofeed.backend.domain.repository.QuizRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @InjectMocks
    private QuizServiceImpl quizService;

    @Test
    @DisplayName("유효한 요청 시, 퀴즈를 조회하고 세부 채점 로직을 호출하여 DTO로 변환한다")
    void shouldEvaluateQuizAndReturnResult_whenValidRequest() {
        // given
        UUID articleId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        
        Quiz mockQuiz = mock(Quiz.class);
        ArticleContent mockContent = mock(ArticleContent.class);
        Article mockArticle = mock(Article.class);

        // 연관관계 모킹 (유효성 검증용)
        given(mockQuiz.getArticleContent()).willReturn(mockContent);
        given(mockContent.getId()).willReturn(contentId);
        given(mockContent.getArticle()).willReturn(mockArticle);
        given(mockArticle.getId()).willReturn(articleId);

        QuizEvaluationResult mockResult = QuizEvaluationResult.builder()
                .isCorrect(true)
                .correctAnswer("Paris")
                .explanation("Paris is the capital of France.")
                .build();
        
        // 도메인 로직은 모킹하여 의존성 분리 (오케스트레이션만 검증)
        given(mockQuiz.evaluate(any(QuizSubmission.class))).willReturn(mockResult);
        given(quizRepository.findById(quizId)).willReturn(Optional.of(mockQuiz));

        QuizAttemptRequest request = QuizAttemptRequest.builder().answerText("Paris").build();

        // when
        QuizAttemptResponse response = quizService.attemptQuiz(articleId, contentId, quizId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isCorrect()).isTrue();
        assertThat(response.correctAnswer()).isEqualTo("Paris");
        assertThat(response.explanation()).isEqualTo("Paris is the capital of France.");
    }

    @Test
    @DisplayName("존재하지 않는 퀴즈 ID로 접근 시 EntityNotFoundException을 발생시킨다")
    void shouldThrowEntityNotFoundException_whenQuizDoesNotExist() {
        // given
        UUID articleId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        QuizAttemptRequest request = QuizAttemptRequest.builder().answerText("Paris").build();

        given(quizRepository.findById(quizId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizService.attemptQuiz(articleId, contentId, quizId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Quiz not found with id: " + quizId);
    }

    @Test
    @DisplayName("퀴즈가 지정된 기사(article)나 콘텐츠(content)에 속하지 않는 경우 IllegalArgumentException 발생")
    void shouldThrowIllegalArgumentException_whenQuizBelongsToDifferentArticleOrContent() {
        // given
        UUID correctArticleId = UUID.randomUUID();
        UUID correctContentId = UUID.randomUUID();
        UUID requestedArticleId = UUID.randomUUID(); // 틀린 ID 입력
        UUID requestedContentId = correctContentId; // Content는 맞더라도
        UUID quizId = UUID.randomUUID();
        
        Quiz mockQuiz = mock(Quiz.class);
        ArticleContent mockContent = mock(ArticleContent.class);
        Article mockArticle = mock(Article.class);

        given(mockQuiz.getArticleContent()).willReturn(mockContent);
        given(mockContent.getId()).willReturn(correctContentId);
        given(mockContent.getArticle()).willReturn(mockArticle);
        given(mockArticle.getId()).willReturn(correctArticleId);

        given(quizRepository.findById(quizId)).willReturn(Optional.of(mockQuiz));
        QuizAttemptRequest request = QuizAttemptRequest.builder().answerText("Paris").build();

        // when & then
        assertThatThrownBy(() -> quizService.attemptQuiz(requestedArticleId, requestedContentId, quizId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quiz does not belong to the specified article or content");
    }
}
