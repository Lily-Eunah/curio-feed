package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles step-level retry for the 3-step generation pipeline.
 *
 * Retry cascade rules (enforced here, not in the controller):
 *   Retry CONTENT   → reset CONTENT, VOCABULARY, QUIZ; clear saved data
 *   Retry VOCABULARY→ reset VOCABULARY, QUIZ; clear vocab/quiz data
 *   Retry QUIZ      → reset QUIZ only; clear quiz data
 *
 * After each retry, the parent SubJob is reset to PENDING so the scheduler
 * will pick it up again.
 */
@Service
public class StepRetryService {

    private static final Logger log = LoggerFactory.getLogger(StepRetryService.class);

    private final ArticleGenerationSubJobRepository subJobRepository;
    private final ArticleGenerationStepJobRepository stepJobRepository;
    private final ArticleContentRepository contentRepository;
    private final VocabularyRepository vocabularyRepository;
    private final QuizRepository quizRepository;

    public StepRetryService(
            ArticleGenerationSubJobRepository subJobRepository,
            ArticleGenerationStepJobRepository stepJobRepository,
            ArticleContentRepository contentRepository,
            VocabularyRepository vocabularyRepository,
            QuizRepository quizRepository) {
        this.subJobRepository = subJobRepository;
        this.stepJobRepository = stepJobRepository;
        this.contentRepository = contentRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.quizRepository = quizRepository;
    }

    @Transactional
    public void retryStep(ArticleGenerationSubJob subJob, GenerationStepType stepType) {
        UUID subJobId = subJob.getId();
        UUID articleId = subJob.getJob().getArticleId();
        DifficultyLevel level = subJob.getLevel();

        switch (stepType) {
            case CONTENT -> {
                // Wipe all step state and saved data; re-run from scratch
                log.info("[telemetry] subJobId={} level={} retryStep=CONTENT invalidatedSteps=[CONTENT,VOCABULARY,QUIZ]",
                        subJobId, level);
                resetOrDeleteStep(subJobId, GenerationStepType.CONTENT);
                resetOrDeleteStep(subJobId, GenerationStepType.VOCABULARY);
                resetOrDeleteStep(subJobId, GenerationStepType.QUIZ);
                clearSavedContent(articleId, level);
            }
            case VOCABULARY -> {
                // Keep CONTENT; wipe VOCABULARY and QUIZ
                log.info("[telemetry] subJobId={} level={} retryStep=VOCABULARY invalidatedSteps=[VOCABULARY,QUIZ]",
                        subJobId, level);
                resetOrDeleteStep(subJobId, GenerationStepType.VOCABULARY);
                resetOrDeleteStep(subJobId, GenerationStepType.QUIZ);
                clearVocabAndQuiz(articleId, level);
            }
            case QUIZ -> {
                // Keep CONTENT and VOCABULARY; wipe QUIZ only
                log.info("[telemetry] subJobId={} level={} retryStep=QUIZ invalidatedSteps=[QUIZ]",
                        subJobId, level);
                resetOrDeleteStep(subJobId, GenerationStepType.QUIZ);
                clearQuizOnly(articleId, level);
            }
        }

        // Reset the parent SubJob to PENDING so the scheduler picks it up
        subJobRepository.resetToPendingWithRetryReset(subJobId);
    }

    // ── Step reset helpers ────────────────────────────────────────────────────

    private void resetOrDeleteStep(UUID subJobId, GenerationStepType stepType) {
        stepJobRepository.findBySubJobIdAndStepType(subJobId, stepType)
                .ifPresent(step -> {
                    step.resetToPending();
                    stepJobRepository.save(step);
                });
    }

    // ── Data-clearing helpers ─────────────────────────────────────────────────

    private void clearSavedContent(UUID articleId, DifficultyLevel level) {
        contentRepository.findByArticleIdAndLevel(articleId, level)
                .ifPresent(ac -> {
                    vocabularyRepository.deleteAllByArticleContentId(ac.getId());
                    quizRepository.deleteAllByArticleContentId(ac.getId());
                    vocabularyRepository.flush();
                    quizRepository.flush();
                    ac.updateContent("");
                    contentRepository.save(ac);
                });
    }

    private void clearVocabAndQuiz(UUID articleId, DifficultyLevel level) {
        contentRepository.findByArticleIdAndLevel(articleId, level)
                .ifPresent(ac -> {
                    vocabularyRepository.deleteAllByArticleContentId(ac.getId());
                    quizRepository.deleteAllByArticleContentId(ac.getId());
                    vocabularyRepository.flush();
                    quizRepository.flush();
                });
    }

    private void clearQuizOnly(UUID articleId, DifficultyLevel level) {
        contentRepository.findByArticleIdAndLevel(articleId, level)
                .ifPresent(ac -> {
                    quizRepository.deleteAllByArticleContentId(ac.getId());
                    quizRepository.flush();
                });
    }
}
