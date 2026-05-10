package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.config.PipelineProperties;
import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

@Component
public class SubJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubJobScheduler.class);

    private final ArticleGenerationSubJobRepository subJobRepository;
    private final SubJobWorker subJobWorker;
    private final ThreeStepSubJobWorker threeStepSubJobWorker;
    private final TaskExecutor subJobTaskExecutor;
    private final PipelineProperties pipelineProperties;

    public SubJobScheduler(
            ArticleGenerationSubJobRepository subJobRepository,
            SubJobWorker subJobWorker,
            ThreeStepSubJobWorker threeStepSubJobWorker,
            @Qualifier("subJobTaskExecutor") TaskExecutor subJobTaskExecutor,
            PipelineProperties pipelineProperties) {
        this.subJobRepository = subJobRepository;
        this.subJobWorker = subJobWorker;
        this.threeStepSubJobWorker = threeStepSubJobWorker;
        this.subJobTaskExecutor = subJobTaskExecutor;
        this.pipelineProperties = pipelineProperties;
    }

    /**
     * fixedDelay: 앞 실행이 완료된 후 대기 → 중복 실행 방지.
     * ai.pipeline.use-three-step=true 이면 ThreeStepSubJobWorker 사용.
     */
    @Scheduled(fixedDelayString = "${ai.pipeline.scheduler-fixed-delay-ms:3000}")
    public void processPending() {
        List<ArticleGenerationSubJob> pendingSubJobs = subJobRepository.findPendingJobs(
                PageRequest.of(0, pipelineProperties.schedulerBatchSize()));

        boolean useThreeStep = pipelineProperties.useThreeStep();

        pendingSubJobs.forEach(subJob -> {
            try {
                subJobTaskExecutor.execute(() -> {
                    try {
                        if (useThreeStep) {
                            threeStepSubJobWorker.process(subJob.getId());
                        } else {
                            subJobWorker.process(subJob.getId());
                        }
                    } catch (Exception e) {
                        log.error("SubJob processing failed: {}", subJob.getId(), e);
                    }
                });
            } catch (RejectedExecutionException e) {
                // 슬롯 없음 → PENDING 상태 유지, 다음 스케줄에 재시도
                log.debug("SubJob skipped (executor full): {}", subJob.getId());
            }
        });
    }
}
