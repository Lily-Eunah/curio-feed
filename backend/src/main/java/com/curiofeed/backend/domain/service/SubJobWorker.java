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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SubJobWorker {

    private static final Logger log = LoggerFactory.getLogger(SubJobWorker.class);
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;

    private final SubJobLockService lockService;
    private final ArticleGenerationSubJobRepository subJobRepository;
    private final ArticleRepository articleRepository;
    private final ArticlePromptBuilder promptBuilder;
    private final LlmClient primaryLlmClient;
    private final LlmClient fallbackLlmClient;
    private final LlmResponseParser responseParser;
    private final GenerationResultValidator validator;
    private final GenerationResultSaver resultSaver;
    private final ArticleStatusAggregator aggregator;
    private final PipelineProperties pipelineProperties;
    private final PipelineMetrics metrics;

    public SubJobWorker(
            SubJobLockService lockService,
            ArticleGenerationSubJobRepository subJobRepository,
            ArticleRepository articleRepository,
            ArticlePromptBuilder promptBuilder,
            LlmClient primaryLlmClient,
            @Qualifier("fallbackLlmClient") LlmClient fallbackLlmClient,
            LlmResponseParser responseParser,
            GenerationResultValidator validator,
            GenerationResultSaver resultSaver,
            ArticleStatusAggregator aggregator,
            PipelineProperties pipelineProperties,
            PipelineMetrics metrics) {
        this.lockService = lockService;
        this.subJobRepository = subJobRepository;
        this.articleRepository = articleRepository;
        this.promptBuilder = promptBuilder;
        this.primaryLlmClient = primaryLlmClient;
        this.fallbackLlmClient = fallbackLlmClient;
        this.responseParser = responseParser;
        this.validator = validator;
        this.resultSaver = resultSaver;
        this.aggregator = aggregator;
        this.pipelineProperties = pipelineProperties;
        this.metrics = metrics;
    }

    public void process(UUID subJobId) {
        // 1. tryLock: false이면 다른 인스턴스가 처리 중 → 즉시 return
        if (!lockService.tryLock(subJobId)) {
            return;
        }

        ArticleGenerationSubJob subJob = subJobRepository.findById(subJobId)
                .orElseThrow(() -> new IllegalArgumentException("SubJob not found: " + subJobId));

        // 2. retryCount 즉시 증가 (lock 획득 = 1회 시도 소비)
        subJob.incrementRetryCount();

        // 3. maxRetryCount 초과 체크
        if (subJob.getRetryCount() > pipelineProperties.maxRetryCount()) {
            markFailed(subJob);
            return;
        }

        UUID articleId = subJob.getJob().getArticleId();
        String originalContent = articleRepository.findById(articleId)
                .map(a -> a.getOriginalContent())
                .orElseThrow(() -> new IllegalStateException("Article not found: " + articleId));

        String prompt = promptBuilder.build(originalContent, subJob.getLevel());
        double threshold = pipelineProperties.thresholdFor(subJob.getLevel());

        // 4. LLM 호출 (트랜잭션 바깥) — heartbeat는 primary + fallback 전체를 커버
        ScheduledExecutorService heartbeatExecutor = startHeartbeat(subJobId);
        GenerationResult result;
        try {
            result = executeWithFallback(prompt, subJob.getLevel(), threshold, subJobId);
        } catch (Exception e) {
            log.warn("[subJob={}] LLM pipeline failed: {}", subJobId, e.getMessage());
            markFailed(subJob);
            return;
        } finally {
            stopHeartbeat(heartbeatExecutor);
        }

        // 5. 저장
        SaveStatus saveStatus = resultSaver.save(articleId, subJob.getLevel(), result);
        if (saveStatus == SaveStatus.FAILED) {
            markFailed(subJob);
            return;
        }

        // 6. 성공
        subJob.updateStatus(JobStatus.COMPLETED);
        aggregateIfTerminal(subJob.getJob().getId(), JobStatus.COMPLETED);
    }

    private GenerationResult executeWithFallback(String prompt, DifficultyLevel level, double threshold, UUID subJobId) {
        // Primary 시도 (gemma)
        GenerationResult primaryResult = null;
        ValidationResult primaryValidation = null;
        try {
            String raw = primaryLlmClient.generate(prompt);
            primaryResult = responseParser.parse(raw, GenerationResult.class);
            primaryValidation = validator.validate(primaryResult);
        } catch (Exception e) {
            log.warn("[subJob={}] Primary LLM failed: {}", subJobId, e.getMessage());
            primaryValidation = ValidationResult.fail(List.of(e.getMessage()), 0.0);
        }

        boolean primaryPassed = primaryValidation.valid() && primaryValidation.score() >= threshold;
        if (primaryPassed) {
            log.debug("[subJob={}] Primary passed (score={})", subJobId,
                    String.format("%.3f", primaryValidation.score()));
            metrics.recordAttempt(level, primaryValidation.score());
            return primaryResult;
        }

        // Fallback 시도 (qwen)
        log.info("[subJob={}] Fallback triggered: valid={}, score={} (threshold={})",
                subJobId, primaryValidation.valid(),
                String.format("%.3f", primaryValidation.score()),
                String.format("%.2f", threshold));
        if (!primaryValidation.errors().isEmpty()) {
            log.debug("[subJob={}] Primary errors: {}", subJobId, primaryValidation.errors());
        }
        metrics.recordFallback(level);

        String fallbackRaw = fallbackLlmClient.generate(prompt);
        metrics.recordQwenUsage();
        GenerationResult fallbackResult = responseParser.parse(fallbackRaw, GenerationResult.class);
        ValidationResult fallbackValidation = validator.validate(fallbackResult);

        metrics.recordAttempt(level, fallbackValidation.score());

        if (!fallbackValidation.valid()) {
            log.warn("[subJob={}] Fallback also failed validation: {}", subJobId, fallbackValidation.errors());
            throw new IllegalStateException("Both primary and fallback failed validation");
        }

        log.info("[subJob={}] Fallback passed (score={})", subJobId,
                String.format("%.3f", fallbackValidation.score()));
        return fallbackResult;
    }

    private void markFailed(ArticleGenerationSubJob subJob) {
        subJob.updateStatus(JobStatus.FAILED);
        aggregateIfTerminal(subJob.getJob().getId(), JobStatus.FAILED);
    }

    // ── Heartbeat ────────────────────────────────────────────────────────────

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

    // ── Helper ───────────────────────────────────────────────────────────────

    private void aggregateIfTerminal(UUID jobId, JobStatus newStatus) {
        if (newStatus == JobStatus.COMPLETED || newStatus == JobStatus.FAILED) {
            aggregator.aggregate(jobId);
        }
    }
}
