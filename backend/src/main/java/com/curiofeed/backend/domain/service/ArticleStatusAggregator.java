package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.Article;
import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.entity.JobStatus;
import com.curiofeed.backend.domain.repository.ArticleGenerationJobRepository;
import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import com.curiofeed.backend.domain.repository.ArticleRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ArticleStatusAggregator {

    private static final int MAX_RETRY = 3;

    private final ArticleGenerationJobRepository jobRepository;
    private final ArticleGenerationSubJobRepository subJobRepository;
    private final ArticleRepository articleRepository;

    public ArticleStatusAggregator(
            ArticleGenerationJobRepository jobRepository,
            ArticleGenerationSubJobRepository subJobRepository,
            ArticleRepository articleRepository) {
        this.jobRepository = jobRepository;
        this.subJobRepository = subJobRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional
    public void aggregate(UUID jobId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        List<ArticleGenerationSubJob> subJobs = subJobRepository.findByJobId(jobId);

        // 진행 중인 SubJob이 있으면 집계 생략
        boolean inProgress = subJobs.stream()
                .anyMatch(s -> s.getStatus() == JobStatus.PENDING || s.getStatus() == JobStatus.PROCESSING);
        if (inProgress) {
            return;
        }

        // 집계 규칙
        boolean anyCompleted = subJobs.stream().anyMatch(s -> s.getStatus() == JobStatus.COMPLETED);
        ArticleStatus targetStatus = anyCompleted ? ArticleStatus.REVIEWING : ArticleStatus.FAILED;

        // 낙관적 락 재시도
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                Article article = articleRepository.findById(job.getArticleId())
                        .orElseThrow(() -> new IllegalArgumentException("Article not found: " + job.getArticleId()));

                if (article.getStatus() == targetStatus) {
                    return; // 이미 동일 상태 → DB write skip
                }

                article.updateStatus(targetStatus);
                articleRepository.saveAndFlush(article);
                return;
            } catch (OptimisticLockException e) {
                if (attempt == MAX_RETRY - 1) {
                    throw new RuntimeException("Article status update failed after retries", e);
                }
            }
        }
    }
}
