package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.EvalScore;
import com.curiofeed.backend.domain.repository.EvalScoreRepository;
import com.curiofeed.backend.infrastructure.llm.EvalPromptBuilder;
import com.curiofeed.backend.infrastructure.llm.LlmClient;
import com.curiofeed.backend.infrastructure.llm.LlmResponseParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class SemanticEvaluatorService {

    private static final Logger log = LoggerFactory.getLogger(SemanticEvaluatorService.class);

    private final EvalScoreRepository evalScoreRepository;
    private final LlmClient primaryLlmClient;
    private final EvalPromptBuilder evalPromptBuilder;
    private final LlmResponseParser responseParser;
    private final double sampleRate;
    private final Random random = new Random();

    public SemanticEvaluatorService(
            EvalScoreRepository evalScoreRepository,
            LlmClient primaryLlmClient,
            EvalPromptBuilder evalPromptBuilder,
            LlmResponseParser responseParser,
            @Value("${curiofeed.eval.sample-rate:0.20}") double sampleRate) {
        this.evalScoreRepository = evalScoreRepository;
        this.primaryLlmClient = primaryLlmClient;
        this.evalPromptBuilder = evalPromptBuilder;
        this.responseParser = responseParser;
        this.sampleRate = sampleRate;
    }

    @Async
    @Transactional
    public void maybeEvaluateAsync(ArticleContent content, String originalArticle) {
        if (random.nextDouble() > sampleRate) {
            log.debug("[SemanticEvaluator] Skipped evaluation based on sampling rate ({})", sampleRate);
            return;
        }

        try {
            log.info("[SemanticEvaluator] Starting LLM-as-Judge evaluation for contentId={}", content.getId());
            DifficultyLevel level = content.getLevel();
            String prompt = evalPromptBuilder.buildEvaluationPrompt(originalArticle, content.getContent(), level);

            String rawResponse = primaryLlmClient.generate(prompt, EvalPromptBuilder.evalSchema());
            JudgeResult result = responseParser.parse(rawResponse, JudgeResult.class);

            EvalScore score = new EvalScore(
                    content,
                    primaryLlmClient.getModelName(),
                    result.factualAccuracy(),
                    result.levelAppropriateness(),
                    result.engagement(),
                    result.safety(),
                    result.overall(),
                    rawResponse
            );

            evalScoreRepository.save(score);
            log.info("[SemanticEvaluator] Saved EvalScore for contentId={}, overallScore={}", content.getId(), result.overall());

        } catch (Exception e) {
            log.error("[SemanticEvaluator] Evaluation failed for contentId={}: {}", content.getId(), e.getMessage(), e);
        }
    }

    public record JudgeResult(
            double factualAccuracy,
            double levelAppropriateness,
            double engagement,
            double safety,
            double overall,
            String explanation
    ) {}
}
