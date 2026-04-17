package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.config.PipelineProperties;
import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.entity.JobStatus;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import com.curiofeed.backend.domain.repository.ArticleRepository;
import com.curiofeed.backend.infrastructure.llm.ArticlePromptBuilder;
import com.curiofeed.backend.infrastructure.llm.LlmClient;
import com.curiofeed.backend.infrastructure.llm.LlmResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private final LlmClient llmClient;
    private final LlmResponseParser responseParser;
    private final GenerationResultSaver resultSaver;
    private final ArticleStatusAggregator aggregator;
    private final PipelineProperties pipelineProperties;

    public SubJobWorker(
            SubJobLockService lockService,
            ArticleGenerationSubJobRepository subJobRepository,
            ArticleRepository articleRepository,
            ArticlePromptBuilder promptBuilder,
            LlmClient llmClient,
            LlmResponseParser responseParser,
            GenerationResultSaver resultSaver,
            ArticleStatusAggregator aggregator,
            PipelineProperties pipelineProperties) {
        this.lockService = lockService;
        this.subJobRepository = subJobRepository;
        this.articleRepository = articleRepository;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.responseParser = responseParser;
        this.resultSaver = resultSaver;
        this.aggregator = aggregator;
        this.pipelineProperties = pipelineProperties;
    }

    public void process(UUID subJobId) {
        // 1. tryLock: false이면 다른 인스턴스가 처리 중 → 즉시 return
        // [트랜잭션 경계: tryLock 내부에서 단일 Tx로 PENDING→PROCESSING UPDATE]
        if (!lockService.tryLock(subJobId)) {
            return;
        }

        ArticleGenerationSubJob subJob = subJobRepository.findById(subJobId)
                .orElseThrow(() -> new IllegalArgumentException("SubJob not found: " + subJobId));

        // 2. retryCount 즉시 증가 (lock 획득 = 1회 시도 소비)
        subJob.incrementRetryCount();

        // 3. maxRetryCount 초과 체크 (early exit: 불필요한 LLM 호출 방지)
        if (subJob.getRetryCount() > pipelineProperties.maxRetryCount()) {
            subJob.updateStatus(JobStatus.FAILED);
            aggregateIfTerminal(subJob.getJob().getId(), JobStatus.FAILED);
            return;
        }

        UUID articleId = subJob.getJob().getArticleId();
        String originalContent = articleRepository.findById(articleId)
                .map(a -> a.getOriginalContent())
                .orElseThrow(() -> new IllegalStateException("Article not found: " + articleId));

        String prompt = promptBuilder.build(originalContent, subJob.getLevel());

        // 4. LLM 호출 (트랜잭션 바깥) — heartbeat는 finally에서 반드시 종료
        ScheduledExecutorService heartbeatExecutor = startHeartbeat(subJobId);
        String llmResponse;
        try {
            llmResponse = llmClient.generate(prompt); // 트랜잭션 바깥
        } catch (Exception e) {
            log.warn("LLM call failed for subJob {}: {}", subJobId, e.getMessage());
            subJob.updateStatus(JobStatus.FAILED);
            aggregateIfTerminal(subJob.getJob().getId(), JobStatus.FAILED);
            return;
        } finally {
            stopHeartbeat(heartbeatExecutor); // 예외/정상 모두 반드시 실행
        }

        // 5. 파싱
        GenerationResult result;
        try {
            result = responseParser.parse(llmResponse, GenerationResult.class);
        } catch (Exception e) {
            log.warn("Parse failed for subJob {}: {}", subJobId, e.getMessage());
            subJob.updateStatus(JobStatus.FAILED);
            aggregateIfTerminal(subJob.getJob().getId(), JobStatus.FAILED);
            return;
        }

        // 6. 저장 — [트랜잭션 경계: GenerationResultSaver 내부 @Transactional]
        SaveStatus saveStatus = resultSaver.save(articleId, subJob.getLevel(), result);
        if (saveStatus == SaveStatus.FAILED) {
            subJob.updateStatus(JobStatus.FAILED);
            aggregateIfTerminal(subJob.getJob().getId(), JobStatus.FAILED);
            return;
        }

        // 7. 성공
        subJob.updateStatus(JobStatus.COMPLETED);
        aggregateIfTerminal(subJob.getJob().getId(), JobStatus.COMPLETED);
    }

    // ── Heartbeat ────────────────────────────────────────────────────────────

    protected ScheduledExecutorService startHeartbeat(UUID subJobId) {
        // Virtual threads are always daemon threads — no need to call daemon(true)
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
