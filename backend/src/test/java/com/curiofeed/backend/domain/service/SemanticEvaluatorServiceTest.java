package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.EvalScore;
import com.curiofeed.backend.domain.repository.EvalScoreRepository;
import com.curiofeed.backend.infrastructure.llm.EvalPromptBuilder;
import com.curiofeed.backend.infrastructure.llm.LlmClient;
import com.curiofeed.backend.infrastructure.llm.LlmResponseParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticEvaluatorServiceTest {

    @Mock
    private EvalScoreRepository evalScoreRepository;

    @Mock
    private LlmClient primaryLlmClient;

    @Mock
    private EvalPromptBuilder evalPromptBuilder;

    @Mock
    private LlmResponseParser responseParser;

    @Test
    @DisplayName("Evaluates content and saves EvalScore when sample rate is 1.0")
    void testEvaluationSavesScore() {
        SemanticEvaluatorService service = new SemanticEvaluatorService(
                evalScoreRepository, primaryLlmClient, evalPromptBuilder, responseParser, 1.0
        );

        ArticleContent mockContent = mock(ArticleContent.class);
        when(mockContent.getLevel()).thenReturn(DifficultyLevel.EASY);
        when(mockContent.getContent()).thenReturn("Simplified AI news article.");
        when(evalPromptBuilder.buildEvaluationPrompt(anyString(), anyString(), any())).thenReturn("eval prompt");
        when(primaryLlmClient.generate(anyString(), any())).thenReturn("raw json");
        when(primaryLlmClient.getModelName()).thenReturn("gemini-1.5-flash");

        SemanticEvaluatorService.JudgeResult result = new SemanticEvaluatorService.JudgeResult(0.9, 0.85, 0.95, 1.0, 0.925, "Good quality");
        when(responseParser.parse(anyString(), eq(SemanticEvaluatorService.JudgeResult.class))).thenReturn(result);

        service.maybeEvaluateAsync(mockContent, "Original AI article text.");

        verify(evalScoreRepository, times(1)).save(any(EvalScore.class));
    }

    @Test
    @DisplayName("Skips evaluation when sample rate is 0.0")
    void testSkippedWhenSampleRateZero() {
        SemanticEvaluatorService service = new SemanticEvaluatorService(
                evalScoreRepository, primaryLlmClient, evalPromptBuilder, responseParser, 0.0
        );

        ArticleContent mockContent = mock(ArticleContent.class);
        service.maybeEvaluateAsync(mockContent, "Original text");

        verifyNoInteractions(evalScoreRepository);
    }
}
