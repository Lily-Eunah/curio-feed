package com.curiofeed.backend.api.dto.admin;

import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.JobStatus;

import java.util.List;
import java.util.UUID;

public record GenerationStatusResponse(
        UUID articleId,
        ArticleStatus articleStatus,
        JobInfo job
) {
    public record JobInfo(UUID jobId, List<SubJobInfo> subJobs) {}

    public record SubJobInfo(DifficultyLevel level, JobStatus status, int retryCount) {}
}
