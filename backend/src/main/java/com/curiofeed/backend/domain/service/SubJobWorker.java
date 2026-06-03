package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.config.PipelineProperties;
import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.JobStatus;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import com.curiofeed.backend.domain.repository.ArticleRepository;
import com.curiofeed.backend.infrastructure.llm.ArticlePromptBuilder;
import com.curiofeed.backend.infrastructure.llm.LlmClient;
import com.curiofeed.backend.infrastructure.llm.LlmResponseParser;
import com.curiofeed.backend.infrastructure.llm.validation.GenerationResultValidator;
import com.curiofeed.backend.infrastructure.llm.validation.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SubJobWorker {

    private static final Logger log = LoggerFactory.getLogger(SubJobWorker.class);
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;
    private static final int MAX_STEP_RETRIES = 2; // Step-level retry limit

    private final SubJobLockService lockService;
    private final ArticleGenerationSubJobRepository subJobRepository;
    private final ArticleRepository articleRepository;
    private final ArticlePromptBuilder promptBuilder;
    private final LlmClient primaryLlmClient;
    private final LlmResponseParser responseParser;
    private final GenerationResultValidator validator;
    private final GenerationResultSaver resultSaver;
    private final ArticleStatusAggregator aggregator;
    private final PipelineProperties pipelineProperties;
    private final PipelineMetrics metrics;
    private final ObjectMapper objectMapper;

    public SubJobWorker(
            SubJobLockService lockService,
            ArticleGenerationSubJobRepository subJobRepository,
            ArticleRepository articleRepository,
            ArticlePromptBuilder promptBuilder,
            LlmClient primaryLlmClient,
            LlmResponseParser responseParser,
            GenerationResultValidator validator,
            GenerationResultSaver resultSaver,
            ArticleStatusAggregator aggregator,
            PipelineProperties pipelineProperties,
            PipelineMetrics metrics,
            ObjectMapper objectMapper) {
        this.lockService = lockService;
        this.subJobRepository = subJobRepository;
        this.articleRepository = articleRepository;
        this.promptBuilder = promptBuilder;
        this.primaryLlmClient = primaryLlmClient;
        this.responseParser = responseParser;
        this.validator = validator;
        this.resultSaver = resultSaver;
        this.aggregator = aggregator;
        this.pipelineProperties = pipelineProperties;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    public void process(UUID subJobId) {
        if (!lockService.tryLock(subJobId)) {
            return;
        }

        ArticleGenerationSubJob subJob = subJobRepository.findByIdWithJob(subJobId)
                .orElseThrow(() -> new IllegalArgumentException("SubJob not found: " + subJobId));

        subJob.incrementRetryCount();
        subJobRepository.save(subJob);

        if (subJob.getRetryCount() > pipelineProperties.maxRetryCount()) {
            markFailed(subJob);
            return;
        }

        UUID articleId = subJob.getJob().getArticleId();
        String originalContent = articleRepository.findById(articleId)
                .map(a -> a.getOriginalContent())
                .orElseThrow(() -> new IllegalStateException("Article not found: " + articleId));

        double threshold = pipelineProperties.thresholdFor(subJob.getLevel());
        ScheduledExecutorService heartbeatExecutor = startHeartbeat(subJobId);
        GenerationResult finalResult;

        try {
            finalResult = executePipeline(originalContent, subJob.getLevel(), threshold, subJobId);
        } catch (Exception e) {
            log.warn("[subJob={}] LLM pipeline failed: {}", subJobId, e.getMessage());
            markFailed(subJob);
            return;
        } finally {
            stopHeartbeat(heartbeatExecutor);
        }

        SaveStatus saveStatus = resultSaver.save(articleId, subJob.getLevel(), finalResult);
        if (saveStatus == SaveStatus.FAILED) {
            markFailed(subJob);
            return;
        }

        subJob.updateStatus(JobStatus.COMPLETED);
        subJobRepository.save(subJob);
        aggregateIfTerminal(subJob.getJob().getId(), JobStatus.COMPLETED);
    }

    private GenerationResult executePipeline(String originalContent, DifficultyLevel level, double threshold, UUID subJobId) throws Exception {
        // Step 0: Source Digest
        GenerationResult.SourceDigestData sourceDigest = null;
        String sourceText = originalContent;
        if (originalContent.split("\\s+").length > 600) {
            log.debug("[subJob={}] Executing Step 0: Source Digest", subJobId);
            GenerationResult step0Result = executeStep(
                    promptBuilder.buildSourceDigestPrompt(originalContent),
                    Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "sourceDigest", Map.of(
                                            "type", "OBJECT",
                                            "properties", Map.of(
                                                    "centralStory", Map.of("type", "STRING"),
                                                    "coreFacts", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                                                    "supportingDetails", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                                                    "omittedDetails", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
                                            )
                                    )
                            )
                    ),
                    subJobId,
                    "Step 0 (Source Digest)"
            );
            if (step0Result == null || !step0Result.hasSourceDigest()) {
                throw new IllegalStateException("Step 0 failed to produce SourceDigest");
            }
            sourceDigest = step0Result.sourceDigest();
            sourceText = objectMapper.writeValueAsString(sourceDigest);
        }

        // Step 1: Content
        log.debug("[subJob={}] Executing Step 1: Content", subJobId);
        GenerationResult step1Result = executeStep(
                promptBuilder.buildContentPrompt(sourceText, level),
                Map.of(
                        "type", "OBJECT",
                        "properties", Map.of("content", Map.of("type", "STRING"))
                ),
                subJobId,
                "Step 1 (Content)"
        );
        if (step1Result == null || !step1Result.hasContent()) {
            throw new IllegalStateException("Step 1 failed to produce Content");
        }
        String content = step1Result.content();

        // Step 2: Vocabulary
        log.debug("[subJob={}] Executing Step 2: Vocabulary", subJobId);
        GenerationResult step2Result = executeStep(
                promptBuilder.buildVocabularyPrompt(content, level),
                Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "vocabularies", Map.of(
                                        "type", "ARRAY",
                                        "items", Map.of(
                                                "type", "OBJECT",
                                                "properties", Map.of(
                                                        "word", Map.of("type", "STRING"),
                                                        "definition", Map.of("type", "STRING"),
                                                        "exampleSentence", Map.of("type", "STRING")
                                                )
                                        )
                                )
                        )
                ),
                subJobId,
                "Step 2 (Vocabulary)"
        );
        if (step2Result == null || !step2Result.hasVocabularies()) {
            throw new IllegalStateException("Step 2 failed to produce Vocabularies");
        }
        List<GenerationResult.VocabularyData> vocabularies = step2Result.vocabularies();

        // Step 3: Quiz
        log.debug("[subJob={}] Executing Step 3: Quiz", subJobId);
        String vocabJson = objectMapper.writeValueAsString(vocabularies);
        GenerationResult step3Result = executeStep(
                promptBuilder.buildQuizPrompt(content, vocabJson),
                null, // Allow arbitrary schema due to complex quiz choices mapping
                subJobId,
                "Step 3 (Quiz)"
        );
        if (step3Result == null || !step3Result.hasQuizzes()) {
            throw new IllegalStateException("Step 3 failed to produce Quizzes");
        }
        List<GenerationResult.QuizData> quizzes = step3Result.quizzes();

        GenerationResult finalResult = new GenerationResult(content, null, vocabularies, quizzes, sourceDigest);

        ValidationResult validation = validator.validate(finalResult);
        metrics.recordAttempt(level, validation.score());

        if (!validation.valid() || validation.score() < threshold) {
            log.warn("[subJob={}] Final Validation failed: score={}, errors={}",
                    subJobId, validation.score(), validation.errors());
            throw new IllegalStateException("Final Pipeline Validation failed");
        }

        log.info("[subJob={}] Pipeline passed (score={})", subJobId, String.format("%.3f", validation.score()));
        return finalResult;
    }

    private GenerationResult executeStep(String prompt, Map<String, Object> schema, UUID subJobId, String stepName) {
        for (int attempt = 1; attempt <= MAX_STEP_RETRIES + 1; attempt++) {
            try {
                String raw = primaryLlmClient.generate(prompt, schema);
                return responseParser.parse(raw, GenerationResult.class);
            } catch (Exception e) {
                log.warn("[subJob={}] {} failed on attempt {}: {}", subJobId, stepName, attempt, e.getMessage());
                if (attempt > MAX_STEP_RETRIES) {
                    throw new IllegalStateException(stepName + " failed after " + MAX_STEP_RETRIES + " retries", e);
                }
            }
        }
        return null; // Should not reach here
    }

    private void markFailed(ArticleGenerationSubJob subJob) {
        subJob.updateStatus(JobStatus.FAILED);
        subJobRepository.save(subJob);
        aggregateIfTerminal(subJob.getJob().getId(), JobStatus.FAILED);
    }

    protected ScheduledExecutorService startHeartbeat(UUID subJobId) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r ->
                Thread.ofVirtual().name("heartbeat-" + subJobId).unstarted(r)
        );
        executor.scheduleAtFixedRate(
                () -> subJobRepository.updateHeartbeat(subJobId, Instant.now()),
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        return executor;
    }

    protected void stopHeartbeat(ScheduledExecutorService executor) {
        executor.shutdownNow();
    }

    private void aggregateIfTerminal(UUID jobId, JobStatus newStatus) {
        if (newStatus == JobStatus.COMPLETED || newStatus == JobStatus.FAILED) {
            aggregator.aggregate(jobId);
        }
    }
}
