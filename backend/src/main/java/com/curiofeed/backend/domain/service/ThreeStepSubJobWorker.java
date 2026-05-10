package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.domain.repository.*;
import com.curiofeed.backend.infrastructure.llm.*;
import com.curiofeed.backend.infrastructure.llm.validation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 3-step generation pipeline worker.
 *
 * Execution order: CONTENT → VOCABULARY → QUIZ
 * Each step is an independent LLM call with its own validator.
 * Intermediate results are saved to ArticleContent/Vocabulary/Quiz tables
 * after each successful step, enabling step-level retry without re-running upstream steps.
 *
 * Dependency rules:
 *   VOCABULARY  depends on CONTENT completed
 *   QUIZ        depends on CONTENT and VOCABULARY completed
 *
 * Retry (triggered via admin API):
 *   Retry CONTENT   → resets CONTENT, VOCABULARY, QUIZ steps; clears saved data
 *   Retry VOCABULARY→ resets VOCABULARY, QUIZ steps; clears vocab/quiz data
 *   Retry QUIZ      → resets QUIZ step only; clears quiz data
 */
@Service
public class ThreeStepSubJobWorker {

    private static final Logger log = LoggerFactory.getLogger(ThreeStepSubJobWorker.class);
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;
    private static final int MAX_STEP_RETRIES = 3;

    private final SubJobLockService lockService;
    private final ArticleGenerationSubJobRepository subJobRepository;
    private final ArticleGenerationStepJobRepository stepJobRepository;
    private final ArticleRepository articleRepository;
    private final ArticleContentRepository contentRepository;
    private final VocabularyRepository vocabularyRepository;
    private final QuizRepository quizRepository;

    private final ThreeStepPromptBuilder promptBuilder;
    private final LlmClient primaryLlmClient;
    private final LlmClient fallbackLlmClient;
    private final LlmResponseParser responseParser;
    private final ObjectMapper objectMapper;

    private final ContentStepValidator contentValidator;
    private final VocabStepValidator vocabValidator;
    private final QuizStepValidator quizValidator;
    private final VocabLemmatizer vocabLemmatizer;

    private final GenerationResultSaver resultSaver;
    private final ArticleStatusAggregator aggregator;

    public ThreeStepSubJobWorker(
            SubJobLockService lockService,
            ArticleGenerationSubJobRepository subJobRepository,
            ArticleGenerationStepJobRepository stepJobRepository,
            ArticleRepository articleRepository,
            ArticleContentRepository contentRepository,
            VocabularyRepository vocabularyRepository,
            QuizRepository quizRepository,
            ThreeStepPromptBuilder promptBuilder,
            LlmClient primaryLlmClient,
            @Qualifier("fallbackLlmClient") LlmClient fallbackLlmClient,
            LlmResponseParser responseParser,
            ObjectMapper objectMapper,
            ContentStepValidator contentValidator,
            VocabStepValidator vocabValidator,
            QuizStepValidator quizValidator,
            VocabLemmatizer vocabLemmatizer,
            GenerationResultSaver resultSaver,
            ArticleStatusAggregator aggregator) {
        this.lockService = lockService;
        this.subJobRepository = subJobRepository;
        this.stepJobRepository = stepJobRepository;
        this.articleRepository = articleRepository;
        this.contentRepository = contentRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.quizRepository = quizRepository;
        this.promptBuilder = promptBuilder;
        this.primaryLlmClient = primaryLlmClient;
        this.fallbackLlmClient = fallbackLlmClient;
        this.responseParser = responseParser;
        this.objectMapper = objectMapper;
        this.contentValidator = contentValidator;
        this.vocabValidator = vocabValidator;
        this.quizValidator = quizValidator;
        this.vocabLemmatizer = vocabLemmatizer;
        this.resultSaver = resultSaver;
        this.aggregator = aggregator;
    }

    public void process(UUID subJobId) {
        if (!lockService.tryLock(subJobId)) {
            return;
        }

        ArticleGenerationSubJob subJob = subJobRepository.findByIdWithJob(subJobId)
                .orElseThrow(() -> new IllegalArgumentException("SubJob not found: " + subJobId));

        subJob.incrementRetryCount();
        subJobRepository.save(subJob);

        ensureStepJobsExist(subJob);

        UUID articleId = subJob.getJob().getArticleId();
        DifficultyLevel level = subJob.getLevel();

        String originalContent = articleRepository.findById(articleId)
                .map(Article::getOriginalContent)
                .orElseThrow(() -> new IllegalStateException("Article not found: " + articleId));

        ScheduledExecutorService heartbeat = startHeartbeat(subJobId);
        try {
            runPipeline(subJob, articleId, level, originalContent);
        } catch (Exception e) {
            log.error("[subJob={}] Unexpected pipeline error: {}", subJobId, e.getMessage(), e);
            markSubJobFailed(subJob);
        } finally {
            stopHeartbeat(heartbeat);
        }
    }

    // ── Pipeline orchestration ────────────────────────────────────────────────

    private void runPipeline(ArticleGenerationSubJob subJob, UUID articleId,
                             DifficultyLevel level, String originalContent) {
        UUID subJobId = subJob.getId();

        // ── Step 1: CONTENT ───────────────────────────────────────────────────
        ArticleGenerationStepJob contentStep = getOrCreateStep(subJob, GenerationStepType.CONTENT);
        String generatedContent;

        if (!contentStep.isCompleted()) {
            generatedContent = executeContentStep(contentStep, subJob, articleId, level, originalContent);
            if (generatedContent == null) return; // hard fail handled inside
        } else {
            generatedContent = contentRepository.findByArticleIdAndLevel(articleId, level)
                    .map(ArticleContent::getContent)
                    .orElseThrow(() -> new IllegalStateException(
                            "CONTENT step is COMPLETED but ArticleContent not found for " + articleId + "/" + level));
            log.info("[subJob={} level={}] CONTENT step already completed, resuming", subJobId, level);
        }

        // ── Step 2: VOCABULARY ────────────────────────────────────────────────
        ArticleGenerationStepJob vocabStep = getOrCreateStep(subJob, GenerationStepType.VOCABULARY);
        List<GenerationResult.VocabularyData> generatedVocab;

        if (!vocabStep.isCompleted()) {
            generatedVocab = executeVocabStep(vocabStep, subJob, articleId, level, generatedContent);
            if (generatedVocab == null) return; // hard fail handled inside
        } else {
            generatedVocab = loadVocabFromDb(articleId, level);
            log.info("[subJob={} level={}] VOCABULARY step already completed, resuming", subJobId, level);
        }

        // ── Step 3: QUIZ ──────────────────────────────────────────────────────
        ArticleGenerationStepJob quizStep = getOrCreateStep(subJob, GenerationStepType.QUIZ);

        if (!quizStep.isCompleted()) {
            boolean quizOk = executeQuizStep(quizStep, subJob, articleId, level, generatedContent, generatedVocab);
            if (!quizOk) return; // hard fail handled inside
        } else {
            log.info("[subJob={} level={}] QUIZ step already completed", subJobId, level);
        }

        // All steps completed
        subJob.updateStatus(JobStatus.COMPLETED);
        subJobRepository.save(subJob);
        aggregator.aggregate(subJob.getJob().getId());
        log.info("[subJob={} level={}] 3-step pipeline COMPLETED", subJobId, level);
    }

    // ── Step executors (each with intra-step retry loop) ─────────────────────

    /**
     * Executes the CONTENT step with up to MAX_STEP_RETRIES attempts.
     * On validation hard fail, selects a corrective retry prompt based on the error.
     *
     * @return the generated content string, or {@code null} if the step ultimately hard-failed.
     */
    private String executeContentStep(ArticleGenerationStepJob step,
                                      ArticleGenerationSubJob subJob,
                                      UUID articleId, DifficultyLevel level,
                                      String originalContent) {
        UUID subJobId = subJob.getId();
        log.info("[subJob={} level={}] Running CONTENT step", subJobId, level);
        step.markProcessing();
        stepJobRepository.save(step);

        String lastErrors = null;
        for (int attempt = 1; attempt <= MAX_STEP_RETRIES; attempt++) {
            try {
                // Choose base or corrective prompt
                String retryReason = classifyContentRetryReason(lastErrors);
                String prompt = (attempt == 1)
                        ? promptBuilder.buildContentPrompt(originalContent, level)
                        : promptBuilder.buildContentRetryPrompt(originalContent, level, retryReason);

                if (attempt > 1) {
                    log.info("[telemetry] articleId={} subJobId={} level={} stepType=CONTENT " +
                                    "attempt={} retryPromptReason={}",
                            articleId, subJobId, level, attempt, retryReason);
                }

                String raw = callLlmWithFallback(prompt, ThreeStepPromptBuilder.contentSchema(), subJobId, "CONTENT");
                GenerationResult result = responseParser.parse(raw, GenerationResult.class);
                String content = result.content();

                List<String> errors = contentValidator.validate(content, level);
                boolean hardFail = contentValidator.isHardFail(errors);
                String errStr = errors.isEmpty() ? null : String.join("; ", errors);

                if (!errors.isEmpty()) {
                    if (hardFail) {
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=CONTENT " +
                                        "attempt={} hardFail=true validationErrors=[{}]",
                                articleId, subJobId, level, attempt, errStr);
                        lastErrors = errStr;
                        if (attempt < MAX_STEP_RETRIES) continue; // retry
                        // Exhausted all retries
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=CONTENT " +
                                        "finalStatus=FAILED totalAttempts={}",
                                articleId, subJobId, level, attempt);
                        step.markFailed("Content validation failed after " + attempt + " attempts", errStr);
                        stepJobRepository.save(step);
                        markSubJobFailed(subJob);
                        return null;
                    } else {
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=CONTENT " +
                                        "attempt={} hardFail=false softWarnings=[{}]",
                                articleId, subJobId, level, attempt, errStr);
                    }
                }

                // Success
                saveContentOnly(articleId, level, content);
                step.markCompleted(hardFail ? "PASS_WITH_WARNINGS" : (errStr != null ? "PASS_WITH_WARNINGS" : "PASS"));
                stepJobRepository.save(step);
                log.info("[telemetry] articleId={} subJobId={} level={} stepType=CONTENT " +
                                "finalStatus=COMPLETED wordCount={} totalAttempts={}",
                        articleId, subJobId, level, content.trim().split("\\s+").length, attempt);
                return content;

            } catch (Exception e) {
                log.warn("[subJob={} level={}] CONTENT attempt {} FAILED: {}", subJobId, level, attempt, e.getMessage());
                lastErrors = e.getMessage();
                if (attempt >= MAX_STEP_RETRIES) {
                    log.warn("[telemetry] articleId={} subJobId={} level={} stepType=CONTENT " +
                                    "finalStatus=FAILED totalAttempts={}",
                            articleId, subJobId, level, attempt);
                    step.markFailed(e.getMessage(), null);
                    stepJobRepository.save(step);
                    markSubJobFailed(subJob);
                    return null;
                }
            }
        }
        return null; // unreachable, but satisfies compiler
    }

    /**
     * Executes the VOCABULARY step with up to MAX_STEP_RETRIES attempts.
     *
     * @return the generated vocab list, or {@code null} if the step ultimately hard-failed.
     */
    private List<GenerationResult.VocabularyData> executeVocabStep(ArticleGenerationStepJob step,
                                                                    ArticleGenerationSubJob subJob,
                                                                    UUID articleId, DifficultyLevel level,
                                                                    String generatedContent) {
        UUID subJobId = subJob.getId();
        log.info("[subJob={} level={}] Running VOCABULARY step", subJobId, level);
        step.markProcessing();
        stepJobRepository.save(step);

        String lastErrors = null;
        for (int attempt = 1; attempt <= MAX_STEP_RETRIES; attempt++) {
            try {
                String retryReason = classifyVocabRetryReason(lastErrors);
                String prompt = (attempt == 1)
                        ? promptBuilder.buildVocabularyPrompt(generatedContent, level)
                        : promptBuilder.buildVocabularyRetryPrompt(generatedContent, level, retryReason);

                if (attempt > 1) {
                    log.info("[telemetry] articleId={} subJobId={} level={} stepType=VOCABULARY " +
                                    "attempt={} retryPromptReason={}",
                            articleId, subJobId, level, attempt, retryReason);
                }

                String raw = callLlmWithFallback(prompt, ThreeStepPromptBuilder.vocabularySchema(), subJobId, "VOCAB");
                GenerationResult result = responseParser.parse(raw, GenerationResult.class);
                List<GenerationResult.VocabularyData> vocabs = result.vocabularies();

                List<String> errors = vocabValidator.validate(vocabs, generatedContent);
                boolean hardFail = vocabValidator.isHardFail(errors);
                String errStr = errors.isEmpty() ? null : String.join("; ", errors);

                if (!errors.isEmpty()) {
                    if (hardFail) {
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=VOCABULARY " +
                                        "attempt={} hardFail=true validationErrors=[{}]",
                                articleId, subJobId, level, attempt, errStr);
                        lastErrors = errStr;
                        if (attempt < MAX_STEP_RETRIES) continue;
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=VOCABULARY " +
                                        "finalStatus=FAILED totalAttempts={}",
                                articleId, subJobId, level, attempt);
                        step.markFailed("Vocab validation failed after " + attempt + " attempts", errStr);
                        stepJobRepository.save(step);
                        markSubJobFailed(subJob);
                        return null;
                    } else {
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=VOCABULARY " +
                                        "attempt={} hardFail=false softWarnings=[{}]",
                                articleId, subJobId, level, attempt, errStr);
                    }
                }

                saveVocabOnly(articleId, level, vocabs);
                step.markCompleted(errStr != null ? "PASS_WITH_WARNINGS" : "PASS");
                stepJobRepository.save(step);
                log.info("[telemetry] articleId={} subJobId={} level={} stepType=VOCABULARY " +
                                "finalStatus=COMPLETED vocabCount={} totalAttempts={}",
                        articleId, subJobId, level, vocabs == null ? 0 : vocabs.size(), attempt);
                return vocabs;

            } catch (Exception e) {
                log.warn("[subJob={} level={}] VOCABULARY attempt {} FAILED: {}", subJobId, level, attempt, e.getMessage());
                lastErrors = e.getMessage();
                if (attempt >= MAX_STEP_RETRIES) {
                    log.warn("[telemetry] articleId={} subJobId={} level={} stepType=VOCABULARY " +
                                    "finalStatus=FAILED totalAttempts={}",
                            articleId, subJobId, level, attempt);
                    step.markFailed(e.getMessage(), null);
                    stepJobRepository.save(step);
                    markSubJobFailed(subJob);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Executes the QUIZ step with up to MAX_STEP_RETRIES attempts.
     * Uses corrective retry prompts for known Q2/Q3 failure modes (even soft-warn),
     * since improving quiz quality on retry has no downstream cost.
     *
     * @return true if the step completed (possibly with warnings), false if hard-failed.
     */
    private boolean executeQuizStep(ArticleGenerationStepJob step,
                                    ArticleGenerationSubJob subJob,
                                    UUID articleId, DifficultyLevel level,
                                    String generatedContent,
                                    List<GenerationResult.VocabularyData> generatedVocab) {
        UUID subJobId = subJob.getId();
        log.info("[subJob={} level={}] Running QUIZ step", subJobId, level);
        step.markProcessing();
        stepJobRepository.save(step);

        String vocabJson = serializeVocab(generatedVocab);
        String lastErrors = null;
        for (int attempt = 1; attempt <= MAX_STEP_RETRIES; attempt++) {
            try {
                String retryReason = classifyQuizRetryReason(lastErrors);
                String prompt = (attempt == 1)
                        ? promptBuilder.buildQuizPrompt(generatedContent, vocabJson, level)
                        : promptBuilder.buildQuizRetryPrompt(generatedContent, vocabJson, level, retryReason);

                if (attempt > 1) {
                    log.info("[telemetry] articleId={} subJobId={} level={} stepType=QUIZ " +
                                    "attempt={} retryPromptReason={}",
                            articleId, subJobId, level, attempt, retryReason);
                }

                String raw = callLlmWithFallback(prompt, ThreeStepPromptBuilder.quizSchema(), subJobId, "QUIZ");
                GenerationResult result = responseParser.parse(raw, GenerationResult.class);

                List<String> errors = quizValidator.validate(result.quizzes(), generatedVocab);
                boolean hardFail = quizValidator.isHardFail(errors);
                String errStr = errors.isEmpty() ? null : String.join("; ", errors);

                if (!errors.isEmpty()) {
                    if (hardFail) {
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=QUIZ " +
                                        "attempt={} hardFail=true validationErrors=[{}]",
                                articleId, subJobId, level, attempt, errStr);
                        lastErrors = errStr;
                        if (attempt < MAX_STEP_RETRIES) continue;
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=QUIZ " +
                                        "finalStatus=FAILED totalAttempts={}",
                                articleId, subJobId, level, attempt);
                        step.markFailed("Quiz validation failed after " + attempt + " attempts", errStr);
                        stepJobRepository.save(step);
                        markSubJobFailed(subJob);
                        return false;
                    } else {
                        // Soft warnings — attempt retry for quality improvement (up to max)
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=QUIZ " +
                                        "attempt={} hardFail=false softWarnings=[{}]",
                                articleId, subJobId, level, attempt, errStr);
                        lastErrors = errStr; // feed into retry reason classifier
                        if (attempt < MAX_STEP_RETRIES) continue; // retry for quality
                        // Max retries reached even for soft — accept with warnings
                        log.info("[telemetry] articleId={} subJobId={} level={} stepType=QUIZ " +
                                        "finalStatus=COMPLETED_WITH_WARNINGS totalAttempts={}",
                                articleId, subJobId, level, attempt);
                        saveQuizOnly(articleId, level, result.quizzes());
                        step.markCompleted("PASS_WITH_WARNINGS");
                        stepJobRepository.save(step);
                        return true;
                    }
                }

                saveQuizOnly(articleId, level, result.quizzes());
                step.markCompleted("PASS");
                stepJobRepository.save(step);
                log.info("[telemetry] articleId={} subJobId={} level={} stepType=QUIZ " +
                                "finalStatus=COMPLETED quizCount={} totalAttempts={}",
                        articleId, subJobId, level, result.quizzes() == null ? 0 : result.quizzes().size(), attempt);
                return true;

            } catch (Exception e) {
                log.warn("[subJob={} level={}] QUIZ attempt {} FAILED: {}", subJobId, level, attempt, e.getMessage());
                lastErrors = e.getMessage();
                if (attempt >= MAX_STEP_RETRIES) {
                    log.warn("[telemetry] articleId={} subJobId={} level={} stepType=QUIZ " +
                                    "finalStatus=FAILED totalAttempts={}",
                            articleId, subJobId, level, attempt);
                    step.markFailed(e.getMessage(), null);
                    stepJobRepository.save(step);
                    markSubJobFailed(subJob);
                    return false;
                }
            }
        }
        return false;
    }

    // ── Retry reason classifiers ──────────────────────────────────────────────

    /** Maps CONTENT validation error string to a retry reason for the corrective prompt. */
    static String classifyContentRetryReason(String errors) {
        if (errors == null) return "unknown";
        String lower = errors.toLowerCase(Locale.ROOT);
        if (lower.contains("too short")) return "too_short";
        if (lower.contains("too long"))  return "too_long";
        return "unknown";
    }

    /** Maps VOCABULARY validation error string to a retry reason. */
    static String classifyVocabRetryReason(String errors) {
        if (errors == null) return "unknown";
        String lower = errors.toLowerCase(Locale.ROOT);
        if (lower.contains("word not found in content")) return "word_not_in_content";
        return "unknown";
    }

    /** Maps QUIZ validation error string to a retry reason. */
    static String classifyQuizRetryReason(String errors) {
        if (errors == null) return "unknown";
        String lower = errors.toLowerCase(Locale.ROOT);
        // Prefer the most actionable known reason
        if (lower.contains("choices do not appear to contain a vocab word")) return "q2_not_vocab_application";
        if (lower.contains("correctanswer") && lower.contains("not found in vocab")) return "q3_answer_not_in_vocab";
        // Fallback: if both Q2 and Q3 issues, prefer Q2 (first fix)
        return "unknown";
    }

    // ── Step-level DB helpers ─────────────────────────────────────────────────

    private void ensureStepJobsExist(ArticleGenerationSubJob subJob) {
        for (GenerationStepType stepType : GenerationStepType.values()) {
            boolean exists = stepJobRepository.findBySubJobIdAndStepType(subJob.getId(), stepType).isPresent();
            if (!exists) {
                stepJobRepository.save(ArticleGenerationStepJob.pending(subJob, stepType));
            }
        }
    }

    private ArticleGenerationStepJob getOrCreateStep(ArticleGenerationSubJob subJob, GenerationStepType stepType) {
        return stepJobRepository.findBySubJobIdAndStepType(subJob.getId(), stepType)
                .orElseGet(() -> {
                    var step = ArticleGenerationStepJob.pending(subJob, stepType);
                    return stepJobRepository.save(step);
                });
    }

    @Transactional
    protected void saveContentOnly(UUID articleId, DifficultyLevel level, String content) {
        ArticleContent articleContent = contentRepository.findByArticleIdAndLevel(articleId, level)
                .orElseGet(() -> {
                    Article article = articleRepository.findById(articleId)
                            .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleId));
                    var ac = ArticleContent.create(article, level, content);
                    return contentRepository.save(ac);
                });
        // Already exists → update content, clear vocab/quiz
        vocabularyRepository.deleteAllByArticleContentId(articleContent.getId());
        quizRepository.deleteAllByArticleContentId(articleContent.getId());
        vocabularyRepository.flush();
        quizRepository.flush();
        articleContent.updateContent(content);
        contentRepository.save(articleContent);
    }

    @Transactional
    protected void saveVocabOnly(UUID articleId, DifficultyLevel level,
                                  List<GenerationResult.VocabularyData> vocabs) {
        if (vocabs == null || vocabs.isEmpty()) return;
        ArticleContent articleContent = contentRepository.findByArticleIdAndLevel(articleId, level)
                .orElseThrow(() -> new IllegalStateException("ArticleContent must exist before saving vocab"));
        vocabularyRepository.deleteAllByArticleContentId(articleContent.getId());
        vocabularyRepository.flush();
        for (var v : vocabs) {
            String displayWord = vocabLemmatizer.normalizeDisplayWord(v.word());
            vocabularyRepository.save(Vocabulary.create(articleContent, displayWord, v.definition(), v.exampleSentence()));
        }
    }

    @Transactional
    protected void saveQuizOnly(UUID articleId, DifficultyLevel level,
                                 List<GenerationResult.QuizData> quizzes) {
        if (quizzes == null || quizzes.isEmpty()) return;
        ArticleContent articleContent = contentRepository.findByArticleIdAndLevel(articleId, level)
                .orElseThrow(() -> new IllegalStateException("ArticleContent must exist before saving quizzes"));
        quizRepository.deleteAllByArticleContentId(articleContent.getId());
        quizRepository.flush();
        for (var q : quizzes) {
            quizRepository.save(Quiz.create(articleContent, q.type(), q.question(), q.options(),
                    q.correctAnswer(), q.explanation()));
        }
    }

    private List<GenerationResult.VocabularyData> loadVocabFromDb(UUID articleId, DifficultyLevel level) {
        return contentRepository.findByArticleIdAndLevel(articleId, level)
                .map(ac -> ac.getVocabularies().stream()
                        .map(v -> new GenerationResult.VocabularyData(v.getWord(), v.getDefinition(), v.getExampleSentence()))
                        .toList())
                .orElseThrow(() -> new IllegalStateException("ArticleContent not found for vocab reload"));
    }

    // ── LLM call with fallback ────────────────────────────────────────────────

    private String callLlmWithFallback(String prompt, Map<String, Object> schema,
                                        UUID subJobId, String stepTag) {
        try {
            return primaryLlmClient.generate(prompt, schema);
        } catch (Exception e) {
            log.warn("[subJob={} {}] Primary LLM failed, trying fallback: {}", subJobId, stepTag, e.getMessage());
        }
        try {
            return fallbackLlmClient.generate(prompt, schema);
        } catch (Exception e) {
            throw new IllegalStateException("Both primary and fallback LLM failed for " + stepTag + ": " + e.getMessage(), e);
        }
    }

    // ── Serialization helpers ─────────────────────────────────────────────────

    private String serializeVocab(List<GenerationResult.VocabularyData> vocabs) {
        try {
            List<Map<String, String>> list = vocabs.stream()
                    .map(v -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("word", v.word());
                        m.put("definition", v.definition());
                        m.put("exampleSentence", v.exampleSentence());
                        return m;
                    }).toList();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize vocab for quiz prompt", e);
        }
    }

    // ── Status helpers ────────────────────────────────────────────────────────

    private void markSubJobFailed(ArticleGenerationSubJob subJob) {
        subJob.updateStatus(JobStatus.FAILED);
        subJobRepository.save(subJob);
        aggregator.aggregate(subJob.getJob().getId());
    }

    // ── Heartbeat ─────────────────────────────────────────────────────────────

    protected ScheduledExecutorService startHeartbeat(UUID subJobId) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r ->
                Thread.ofVirtual().name("heartbeat-3step-" + subJobId).unstarted(r));
        executor.scheduleAtFixedRate(
                () -> subJobRepository.updateHeartbeat(subJobId, Instant.now()),
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        return executor;
    }

    protected void stopHeartbeat(ScheduledExecutorService executor) {
        executor.shutdownNow();
    }
}
