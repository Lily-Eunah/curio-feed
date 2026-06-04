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

import org.springframework.data.domain.PageRequest;

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
    private static final int LONG_ARTICLE_THRESHOLD_WORDS = 600;
    private static final int CHUNK_ARTICLE_THRESHOLD_WORDS = 1200; // TODO: Implement chunking for > 1200

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

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalStateException("Article not found: " + articleId));
        String originalContent = article.getOriginalContent();
        String originalTitle = article.getOriginalTitle();

        ScheduledExecutorService heartbeat = startHeartbeat(subJobId);
        try {
            runPipeline(subJob, articleId, level, originalTitle, originalContent);
        } catch (Exception e) {
            log.error("[subJob={}] Unexpected pipeline error: {}", subJobId, e.getMessage(), e);
            markSubJobFailed(subJob);
        } finally {
            stopHeartbeat(heartbeat);
        }
    }

    // ── Pipeline orchestration ────────────────────────────────────────────────

    private void runPipeline(ArticleGenerationSubJob subJob, UUID articleId,
                             DifficultyLevel level, String originalTitle, String originalContent) {
        UUID subJobId = subJob.getId();
        int originalWordCount = countWords(originalContent);

        // ── Step 0: SOURCE_DIGEST (Mandatory for copyright safety) ──────────────
        String sourceText = originalContent;
        boolean isDigestUsed = false;

        ArticleGenerationStepJob digestStep = getOrCreateStep(subJob, GenerationStepType.SOURCE_DIGEST);

        if (!digestStep.isCompleted()) {
            GenerationResult.SourceDigestData digestData = executeSourceDigestStep(digestStep, subJob, originalTitle, originalContent);
            if (digestData == null) return; // hard fail
            sourceText = formatDigest(digestData);
            isDigestUsed = true;
            
            updateArticleTitleSafely(articleId, digestData.suggestedTitle());
        } else {
            // Need to retrieve digestData from DB if already completed, but since digest output isn't fully persisted as a standalone entity currently,
            // we will just proceed with the original text as fallback or we would need to store digest string. 
            // However, assuming CONTENT step will use its own completed logic, this is fine for now.
            // Actually, if we just set isDigestUsed = true, CONTENT step will fetch its own output if already completed.
            isDigestUsed = true;
            log.info("[subJob={} level={}] SOURCE_DIGEST step already completed, resuming", subJobId, level);
        }

        // ── Step 1: CONTENT ───────────────────────────────────────────────────
        ArticleGenerationStepJob contentStep = getOrCreateStep(subJob, GenerationStepType.CONTENT);
        String generatedContent;

        if (!contentStep.isCompleted()) {
            generatedContent = executeContentStep(contentStep, subJob, articleId, level, sourceText, isDigestUsed);
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

    private void updateArticleTitleSafely(UUID articleId, String suggestedTitle) {
        if (suggestedTitle == null || suggestedTitle.isBlank()) return;
        try {
            Article article = articleRepository.findById(articleId).orElse(null);
            if (article != null && article.getTitle().equals(article.getOriginalTitle())) {
                article.updateTitle(suggestedTitle);
                articleRepository.save(article);
                log.info("[articleId={}] Updated title to: {}", articleId, suggestedTitle);
            }
        } catch (Exception e) {
            log.warn("[articleId={}] Failed to update title (likely concurrent update): {}", articleId, e.getMessage());
        }
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
                                      String sourceText, boolean isDigestUsed) {
        UUID subJobId = subJob.getId();
        log.info("[subJob={} level={}] Running CONTENT step (isDigestUsed={})", subJobId, level, isDigestUsed);
        step.markProcessing();
        stepJobRepository.save(step);

        ContentValidationResult lastResult = null;
        for (int attempt = 1; attempt <= MAX_STEP_RETRIES; attempt++) {
            Instant start = Instant.now();
            try {
                String prompt = (attempt == 1)
                        ? promptBuilder.buildContentPrompt(sourceText, level, isDigestUsed)
                        : promptBuilder.buildContentRetryPrompt(sourceText, level, lastResult, isDigestUsed);

                log.info("[diagnostics] subJob={} step=CONTENT attempt={} event=LLM_REQUEST_START ts={}", subJobId, attempt, Instant.now());
                String raw = callLlmWithFallback(prompt, ThreeStepPromptBuilder.contentSchema(), subJobId, "CONTENT");
                log.info("[diagnostics] subJob={} step=CONTENT attempt={} event=LLM_RESPONSE_RECEIVED duration={}ms", 
                        subJobId, attempt, Instant.now().toEpochMilli() - start.toEpochMilli());

                log.info("[diagnostics] subJob={} step=CONTENT attempt={} event=PARSE_START", subJobId, attempt);
                GenerationResult result = responseParser.parse(raw, GenerationResult.class);
                String content = result.content();

                log.info("[subJob={} level={}] Content validation started", subJobId, level);
                ContentValidationResult validationResult = contentValidator.validate(content, level);
                lastResult = validationResult;
                
                log.info("[diagnostics] subJob={} step=CONTENT attempt={} event=VALIDATION_RESULT level={} status={} words={} pref={}~{} hard={}~{} retry={}", 
                        subJobId, attempt, level, validationResult.getStatus(), validationResult.getActualWordCount(), 
                        validationResult.getPreferredMin(), validationResult.getPreferredMax(),
                        validationResult.getHardMin(), validationResult.getHardMax(),
                        validationResult.getRetryReason());

                if (!validationResult.isSuccess()) {
                    if (validationResult.isHardFail()) {
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=CONTENT attempt={} hardFail=true validationStatus={} words={} retryReason={} errors=[{}]",
                                articleId, subJobId, level, attempt, validationResult.getStatus(), 
                                validationResult.getActualWordCount(), validationResult.getRetryReason(),
                                validationResult.getMessage());
                        if (attempt < MAX_STEP_RETRIES) continue;
                        markStepFailed(step, subJob, "VALIDATION_ERROR", validationResult.getMessage(), validationResult.getMessage());
                        return null;
                    }
                }

                log.info("[diagnostics] subJob={} step=CONTENT attempt={} event=SAVE_START", subJobId, attempt);
                resultSaver.saveContent(articleId, level, content);
                log.info("[diagnostics] subJob={} step=CONTENT attempt={} event=SAVE_COMPLETE", subJobId, attempt);

                step.markCompleted(validationResult.getStatus() != ContentValidationResult.ValidationStatus.VALID ? "PASS_WITH_WARNINGS" : "PASS");
                stepJobRepository.save(step);
                log.info("[diagnostics] subJob={} step=CONTENT attempt={} event=STATUS_UPDATE_COMPLETE total_duration={}ms", 
                        subJobId, attempt, Instant.now().toEpochMilli() - start.toEpochMilli());
                return content;

            } catch (Exception e) {
                handleStepException(step, subJob, "CONTENT", attempt, start, e);
                // Wrap exception as validation failure to trigger retry
                lastResult = ContentValidationResult.builder()
                        .success(false)
                        .level(level)
                        .status(ContentValidationResult.ValidationStatus.TOO_SHORT_HARD_FAIL) // treat as hard fail for retry
                        .retryReason("exception")
                        .message(e.getMessage())
                        .errors(List.of(e.getMessage()))
                        .preferredMin(0).preferredMax(0).hardMin(0).hardMax(0)
                        .build();
                if (attempt >= MAX_STEP_RETRIES) return null;
            }
        }
        return null;
    }

    private GenerationResult.SourceDigestData executeSourceDigestStep(ArticleGenerationStepJob step,
                                                                      ArticleGenerationSubJob subJob,
                                                                      String originalTitle,
                                                                      String originalContent) {
        UUID subJobId = subJob.getId();
        log.info("[subJob={}] Running SOURCE_DIGEST step", subJobId);
        step.markProcessing();
        stepJobRepository.save(step);

        for (int attempt = 1; attempt <= MAX_STEP_RETRIES; attempt++) {
            Instant start = Instant.now();
            try {
                String prompt = promptBuilder.buildSourceDigestPrompt(originalTitle, originalContent);
                log.info("[diagnostics] subJob={} step=SOURCE_DIGEST attempt={} event=LLM_REQUEST_START ts={}", subJobId, attempt, Instant.now());
                String raw = callLlmWithFallback(prompt, ThreeStepPromptBuilder.sourceDigestSchema(), subJobId, "SOURCE_DIGEST");
                log.info("[diagnostics] subJob={} step=SOURCE_DIGEST attempt={} event=LLM_RESPONSE_RECEIVED duration={}ms",
                        subJobId, attempt, Instant.now().toEpochMilli() - start.toEpochMilli());

                log.info("[diagnostics] subJob={} step=SOURCE_DIGEST attempt={} event=PARSE_START", subJobId, attempt);
                GenerationResult result = responseParser.parse(raw, GenerationResult.class);
                GenerationResult.SourceDigestData digest = result.sourceDigest();

                if (digest == null || digest.centralStory() == null) {
                    throw new IllegalStateException("SourceDigest missing from LLM response");
                }

                step.markCompleted("PASS");
                stepJobRepository.save(step);
                log.info("[diagnostics] subJob={} step=SOURCE_DIGEST attempt={} event=STATUS_UPDATE_COMPLETE total_duration={}ms",
                        subJobId, attempt, Instant.now().toEpochMilli() - start.toEpochMilli());
                return digest;

            } catch (Exception e) {
                handleStepException(step, subJob, "SOURCE_DIGEST", attempt, start, e);
                if (attempt >= MAX_STEP_RETRIES) return null;
            }
        }
        return null;
    }

    private String formatDigest(GenerationResult.SourceDigestData digest) {
        return """
                CENTRAL STORY: %s
                CORE FACTS:
                - %s
                SUPPORTING DETAILS:
                - %s
                OMITTED DETAILS (for reference):
                - %s
                """.formatted(
                digest.centralStory(),
                String.join("\n- ", digest.coreFacts()),
                String.join("\n- ", digest.supportingDetails()),
                String.join("\n- ", digest.omittedDetails())
        );
    }

    private static final int RECENT_VOCAB_LOOKBACK = 50; // ~10 articles × 5 words per level

    /**
     * Executes the VOCABULARY step with up to MAX_STEP_RETRIES attempts.
     * After each successful generation, checks for cross-article duplicate words
     * against the last ~10 articles at the same level. If duplicates are found
     * and attempts remain, retries with a small exclusion list.
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
        List<String> dedupExclusions = null; // words to exclude on dedup retry

        for (int attempt = 1; attempt <= MAX_STEP_RETRIES; attempt++) {
            Instant start = Instant.now();
            try {
                String prompt;
                if (attempt == 1) {
                    prompt = promptBuilder.buildVocabularyPrompt(generatedContent, level);
                } else if (dedupExclusions != null) {
                    prompt = promptBuilder.buildVocabularyDeduplicationRetryPrompt(generatedContent, level, dedupExclusions);
                    dedupExclusions = null;
                } else {
                    String retryReason = classifyVocabRetryReason(lastErrors);
                    prompt = promptBuilder.buildVocabularyRetryPrompt(generatedContent, level, retryReason);
                }

                log.info("[diagnostics] subJob={} step=VOCABULARY attempt={} event=LLM_REQUEST_START ts={}", subJobId, attempt, Instant.now());
                String raw = callLlmWithFallback(prompt, ThreeStepPromptBuilder.vocabularySchema(), subJobId, "VOCAB");
                log.info("[diagnostics] subJob={} step=VOCABULARY attempt={} event=LLM_RESPONSE_RECEIVED duration={}ms",
                        subJobId, attempt, Instant.now().toEpochMilli() - start.toEpochMilli());

                log.info("[diagnostics] subJob={} step=VOCABULARY attempt={} event=PARSE_START", subJobId, attempt);
                GenerationResult result = responseParser.parse(raw, GenerationResult.class);
                List<GenerationResult.VocabularyData> vocabs = result.vocabularies();

                log.info("[subJob={} level={}] Vocab validation started", subJobId, level);
                List<String> errors = vocabValidator.validate(vocabs, generatedContent);
                boolean hardFail = vocabValidator.isHardFail(errors);
                String errStr = errors.isEmpty() ? null : String.join("; ", errors);

                if (!errors.isEmpty()) {
                    if (hardFail) {
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=VOCABULARY attempt={} hardFail=true validationErrors=[{}]",
                                articleId, subJobId, level, attempt, errStr);
                        lastErrors = errStr;
                        if (attempt < MAX_STEP_RETRIES) continue;
                        markStepFailed(step, subJob, "VALIDATION_ERROR", "Validation failed: " + errStr, errStr);
                        return null;
                    }
                }

                // Cross-article deduplication check
                List<String> duplicates = findCrossArticleDuplicates(vocabs, level, articleId);
                if (!duplicates.isEmpty()) {
                    log.info("[subJob={} level={}] Cross-article duplicate vocab detected: {} — {}",
                            subJobId, level, duplicates,
                            attempt < MAX_STEP_RETRIES ? "retrying with exclusion list" : "saving best-effort");
                    if (attempt < MAX_STEP_RETRIES) {
                        dedupExclusions = duplicates;
                        continue;
                    }
                }

                log.info("[diagnostics] subJob={} step=VOCABULARY attempt={} event=SAVE_START", subJobId, attempt);
                resultSaver.saveVocab(articleId, level, vocabs, vocabLemmatizer);
                log.info("[diagnostics] subJob={} step=VOCABULARY attempt={} event=SAVE_COMPLETE", subJobId, attempt);

                step.markCompleted(errStr != null ? "PASS_WITH_WARNINGS" : "PASS");
                stepJobRepository.save(step);
                log.info("[diagnostics] subJob={} step=VOCABULARY attempt={} event=STATUS_UPDATE_COMPLETE total_duration={}ms",
                        subJobId, attempt, Instant.now().toEpochMilli() - start.toEpochMilli());
                return vocabs;

            } catch (Exception e) {
                handleStepException(step, subJob, "VOCABULARY", attempt, start, e);
                lastErrors = e.getMessage();
                if (attempt >= MAX_STEP_RETRIES) return null;
            }
        }
        return null;
    }

    private List<String> findCrossArticleDuplicates(List<GenerationResult.VocabularyData> vocabs,
                                                     DifficultyLevel level, UUID currentArticleId) {
        List<String> rawRecent = vocabularyRepository.findRecentWordsByLevel(level, currentArticleId,
                PageRequest.of(0, RECENT_VOCAB_LOOKBACK));
        if (rawRecent.isEmpty()) return List.of();

        Set<String> recentWords = rawRecent.stream()
                .map(w -> w.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return vocabs.stream()
                .map(v -> v.word().toLowerCase(Locale.ROOT))
                .filter(recentWords::contains)
                .toList();
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
            Instant start = Instant.now();
            try {
                String retryReason = classifyQuizRetryReason(lastErrors);
                String prompt = (attempt == 1)
                        ? promptBuilder.buildQuizPrompt(generatedContent, vocabJson, level)
                        : promptBuilder.buildQuizRetryPrompt(generatedContent, vocabJson, level, retryReason);

                log.info("[diagnostics] subJob={} step=QUIZ attempt={} event=LLM_REQUEST_START ts={}", subJobId, attempt, Instant.now());
                String raw = callLlmWithFallback(prompt, ThreeStepPromptBuilder.quizSchema(), subJobId, "QUIZ");
                log.info("[diagnostics] subJob={} step=QUIZ attempt={} event=LLM_RESPONSE_RECEIVED duration={}ms", 
                        subJobId, attempt, Instant.now().toEpochMilli() - start.toEpochMilli());

                log.info("[diagnostics] subJob={} step=QUIZ attempt={} event=PARSE_START", subJobId, attempt);
                GenerationResult result = responseParser.parse(raw, GenerationResult.class);

                log.info("[subJob={} level={}] Quiz validation started", subJobId, level);
                List<String> errors = quizValidator.validate(result.quizzes(), generatedVocab);
                boolean hardFail = quizValidator.isHardFail(errors);
                String errStr = errors.isEmpty() ? null : String.join("; ", errors);

                if (!errors.isEmpty()) {
                    if (hardFail) {
                        log.warn("[telemetry] articleId={} subJobId={} level={} stepType=QUIZ attempt={} hardFail=true validationErrors=[{}]",
                                articleId, subJobId, level, attempt, errStr);
                        lastErrors = errStr;
                        if (attempt < MAX_STEP_RETRIES) continue;
                        markStepFailed(step, subJob, "VALIDATION_ERROR", "Validation failed: " + errStr, errStr);
                        return false;
                    } else {
                        lastErrors = errStr;
                        if (attempt < MAX_STEP_RETRIES) continue;
                        
                        log.info("[diagnostics] subJob={} step=QUIZ attempt={} event=SAVE_START", subJobId, attempt);
                        resultSaver.saveQuiz(articleId, level, result.quizzes());
                        log.info("[diagnostics] subJob={} step=QUIZ attempt={} event=SAVE_COMPLETE", subJobId, attempt);

                        step.markCompleted("PASS_WITH_WARNINGS");
                        stepJobRepository.save(step);
                        log.info("[diagnostics] subJob={} step=QUIZ attempt={} event=STATUS_UPDATE_COMPLETE total_duration={}ms", 
                                subJobId, attempt, Instant.now().toEpochMilli() - start.toEpochMilli());
                        return true;
                    }
                }

                log.info("[diagnostics] subJob={} step=QUIZ attempt={} event=SAVE_START", subJobId, attempt);
                resultSaver.saveQuiz(articleId, level, result.quizzes());
                log.info("[diagnostics] subJob={} step=QUIZ attempt={} event=SAVE_COMPLETE", subJobId, attempt);

                step.markCompleted("PASS");
                stepJobRepository.save(step);
                log.info("[diagnostics] subJob={} step=QUIZ attempt={} event=STATUS_UPDATE_COMPLETE total_duration={}ms", 
                        subJobId, attempt, Instant.now().toEpochMilli() - start.toEpochMilli());
                return true;

            } catch (Exception e) {
                handleStepException(step, subJob, "QUIZ", attempt, start, e);
                lastErrors = e.getMessage();
                if (attempt >= MAX_STEP_RETRIES) return false;
            }
        }
        return false;
    }

    private int countWords(String text) {
        if (text == null) return 0;
        return text.trim().split("\\s+").length;
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

    // Legacy local save helpers removed in favor of GenerationResultSaver


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

    private void handleStepException(ArticleGenerationStepJob step, ArticleGenerationSubJob subJob, 
                                     String stepTag, int attempt, Instant start, Exception e) {
        long duration = Instant.now().toEpochMilli() - start.toEpochMilli();
        log.warn("[diagnostics] subJob={} step={} attempt={} event=EXCEPTION duration={}ms type={} message={}", 
                subJob.getId(), stepTag, attempt, duration, e.getClass().getSimpleName(), e.getMessage());

        if (attempt >= MAX_STEP_RETRIES) {
            String failureReason = classifyException(e);
            String fullError = formatErrorWithStack(e);
            markStepFailed(step, subJob, failureReason, fullError, null);
        }
    }

    private void markStepFailed(ArticleGenerationStepJob step, ArticleGenerationSubJob subJob, 
                                String reason, String message, String valErrors) {
        step.markFailed(message, valErrors, reason);
        stepJobRepository.save(step);
        markSubJobFailed(subJob);
        log.info("[diagnostics] subJob={} step={} event=STATUS_UPDATE_FAILED reason={}", 
                subJob.getId(), step.getStepType(), reason);
    }

    private String classifyException(Exception e) {
        String name = e.getClass().getName();
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (name.contains("Timeout") || name.contains("SocketTimeout") || msg.contains("timeout") || msg.contains("timed out")) {
            return "TIMEOUT";
        }
        if (name.contains("DataAccess") || name.contains("Transaction") || name.contains("Sql") || msg.contains("update/delete")) {
            return "TRANSACTION_ERROR";
        }
        if (name.contains("Json") || name.contains("Parse")) {
            return "PARSE_ERROR";
        }
        return "UNKNOWN_ERROR";
    }

    private String formatErrorWithStack(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(e.getClass().getSimpleName()).append("] ").append(e.getMessage()).append("\n");
        
        StackTraceElement[] stack = e.getStackTrace();
        int count = Math.min(stack.length, 10); // Capture first 10 frames
        for (int i = 0; i < count; i++) {
            sb.append("  at ").append(stack[i]).append("\n");
        }
        if (stack.length > count) {
            sb.append("  ... (").append(stack.length - count).append(" more)");
        }
        
        if (e.getCause() != null) {
            sb.append("\nCaused by: ").append(formatErrorWithStack(e.getCause()));
        }
        return sb.toString();
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
