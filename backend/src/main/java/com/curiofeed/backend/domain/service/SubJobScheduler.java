package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.config.PipelineProperties;
import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.event.ArticleIngestedEvent;
import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
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
    private final boolean schedulerEnabled;

    public SubJobScheduler(
            ArticleGenerationSubJobRepository subJobRepository,
            SubJobWorker subJobWorker,
            ThreeStepSubJobWorker threeStepSubJobWorker,
            @Qualifier("subJobTaskExecutor") TaskExecutor subJobTaskExecutor,
            PipelineProperties pipelineProperties,
            @Value("${ai.pipeline.scheduler-enabled:false}") boolean schedulerEnabled) {
        this.subJobRepository = subJobRepository;
        this.subJobWorker = subJobWorker;
        this.threeStepSubJobWorker = threeStepSubJobWorker;
        this.subJobTaskExecutor = subJobTaskExecutor;
        this.pipelineProperties = pipelineProperties;
        this.schedulerEnabled = schedulerEnabled;
    }

    /**
     * Periodic background polling loop.
     * Controlled by ai.pipeline.scheduler-enabled property (defaults to false in production for Neon auto-suspend).
     */
    @Scheduled(fixedDelayString = "${ai.pipeline.scheduler-fixed-delay-ms:3000}")
    public void scheduledLoop() {
        if (!schedulerEnabled) {
            return; // Skip polling when background loop is disabled
        }
        processPending();
    }

    /**
     * Event-driven trigger: fired immediately when a new article job is registered.
     */
    @EventListener
    public void onArticleIngested(ArticleIngestedEvent event) {
        log.info("[SubJobScheduler] Event-driven trigger received for job={}, triggering processing", event.jobId());
        subJobTaskExecutor.execute(this::processAllPendingUntilEmpty);
    }

    /**
     * Continuously processes PENDING sub-jobs until queue is empty, then goes to idle.
     */
    public void processAllPendingUntilEmpty() {
        List<ArticleGenerationSubJob> pending;
        do {
            processPending();
            pending = subJobRepository.findPendingJobs(PageRequest.of(0, pipelineProperties.schedulerBatchSize()));
            if (!pending.isEmpty()) {
                try {
                    Thread.sleep(500); // Short pause before draining remaining jobs
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } while (!pending.isEmpty());
        log.info("[SubJobScheduler] All PENDING sub-jobs processed. Going to sleep (Neon auto-suspend friendly).");
    }

    /**
     * Process a batch of pending sub-jobs.
     */
    public void processPending() {
        List<ArticleGenerationSubJob> pendingSubJobs = subJobRepository.findPendingJobs(
                PageRequest.of(0, pipelineProperties.schedulerBatchSize()));

        if (pendingSubJobs.isEmpty()) {
            return;
        }

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
                log.debug("SubJob skipped (executor full): {}", subJob.getId());
            }
        });
    }
}
