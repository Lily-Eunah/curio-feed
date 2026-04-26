package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.config.PipelineProperties;
import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.entity.JobStatus;
import com.curiofeed.backend.domain.repository.ArticleGenerationJobRepository;
import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    private final ArticleGenerationSubJobRepository subJobRepository;
    private final ArticleGenerationJobRepository jobRepository;
    private final ArticleStatusAggregator aggregator;
    private final PipelineProperties pipelineProperties;

    public ReconciliationScheduler(
            ArticleGenerationSubJobRepository subJobRepository,
            ArticleGenerationJobRepository jobRepository,
            ArticleStatusAggregator aggregator,
            PipelineProperties pipelineProperties) {
        this.subJobRepository = subJobRepository;
        this.jobRepository = jobRepository;
        this.aggregator = aggregator;
        this.pipelineProperties = pipelineProperties;
    }

    /**
     * Stale PROCESSING SubJob 복구: 1분마다 실행.
     * threshold 이상 heartbeat가 없으면:
     *  - retryCount < maxRetryCount → PENDING (Worker가 다음 스케줄에 재시도)
     *  - retryCount >= maxRetryCount → FAILED
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void recoverStaleJobs() {
        Instant threshold = Instant.now().minus(pipelineProperties.staleJobThresholdMinutes(), ChronoUnit.MINUTES);
        List<ArticleGenerationSubJob> staleJobs = subJobRepository.findStaleProcessingJobs(threshold);

        for (ArticleGenerationSubJob subJob : staleJobs) {
            if (subJob.getRetryCount() < pipelineProperties.maxRetryCount()) {
                log.info("Recovering stale SubJob {} → PENDING (retryCount={})",
                        subJob.getId(), subJob.getRetryCount());
                // JPQL direct UPDATE (예외적 허용: lock 획득 외 stale 복구 목적)
                subJobRepository.resetToPending(subJob.getId());
            } else {
                log.warn("Marking stale SubJob {} → FAILED (retryCount={} >= maxRetry={})",
                        subJob.getId(), subJob.getRetryCount(), pipelineProperties.maxRetryCount());
                subJobRepository.forceSetStatus(subJob.getId(), JobStatus.FAILED);
                aggregator.aggregate(subJob.getJob().getId());
            }
        }
    }

    /**
     * Article 상태 재집계: 1분마다 실행.
     * 모든 SubJob이 terminal state인데 Article이 아직 DRAFT/FAILED인 경우 재집계.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void reconcileArticleStatus() {
        jobRepository.findJobsPendingReconciliation()
                .forEach(job -> {
                    log.info("Reconciling article status for job {}", job.getId());
                    aggregator.aggregate(job.getId());
                });
    }
}
