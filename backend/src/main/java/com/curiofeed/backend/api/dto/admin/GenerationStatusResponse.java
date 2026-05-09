package com.curiofeed.backend.api.dto.admin;

import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.GenerationStepType;
import com.curiofeed.backend.domain.entity.JobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GenerationStatusResponse(
        UUID articleId,
        ArticleStatus articleStatus,
        JobInfo job
) {
    public record JobInfo(UUID jobId, List<SubJobInfo> subJobs) {}

    public record SubJobInfo(
            UUID subJobId,
            DifficultyLevel level,
            JobStatus status,
            int retryCount,
            Instant lastHeartbeatAt,
            List<StepJobInfo> steps
    ) {}

    public record StepJobInfo(
            UUID stepJobId,
            GenerationStepType stepType,
            JobStatus status,
            int attemptCount,
            Instant startedAt,
            Instant completedAt,
            String validationStatus,
            String validationErrors,
            String errorMessage
    ) {}
}
