package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.admin.RegisterArticleRequest;
import com.curiofeed.backend.api.dto.admin.RegisterArticleResponse;
import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleIngestionService {

    private final ArticleRepository articleRepository;
    private final ArticleGenerationJobRepository jobRepository;
    private final ArticleGenerationSubJobRepository subJobRepository;
    private final ArticleGenerationStepJobRepository stepJobRepository;
    private final CategoryRepository categoryRepository;
    private final SlugGenerator slugGenerator;
    private final MeterRegistry meterRegistry;

    public ArticleIngestionService(
            ArticleRepository articleRepository,
            ArticleGenerationJobRepository jobRepository,
            ArticleGenerationSubJobRepository subJobRepository,
            ArticleGenerationStepJobRepository stepJobRepository,
            CategoryRepository categoryRepository,
            SlugGenerator slugGenerator,
            MeterRegistry meterRegistry) {
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
        this.subJobRepository = subJobRepository;
        this.stepJobRepository = stepJobRepository;
        this.categoryRepository = categoryRepository;
        this.slugGenerator = slugGenerator;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 기사 등록 + Job 생성.
     * sourceUrl 기반 idempotency: 이미 존재하면 ConflictException throw.
     */
    @Transactional
    public RegisterArticleResponse register(RegisterArticleRequest request) {
        long startTimeMs = System.currentTimeMillis();
        try {
            return doRegister(request, startTimeMs);
        } catch (ArticleAlreadyExistsException e) {
            recordIngestionMetric("duplicate", System.currentTimeMillis() - startTimeMs);
            throw e;
        } catch (RuntimeException e) {
            recordIngestionMetric("error", System.currentTimeMillis() - startTimeMs);
            throw e;
        }
    }

    private RegisterArticleResponse doRegister(RegisterArticleRequest request, long startTimeMs) {
        // 1. 중복 확인
        Optional<Article> existing = articleRepository.findBySourceUrl(request.sourceUrl());
        if (existing.isPresent()) {
            throw new ArticleAlreadyExistsException("Article already exists", existing.get().getId());
        }

        // 2. Category 조회
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.categoryId()));

        // 3. Article 생성
        String slug = slugGenerator.generate(request.originalTitle());
        Article article = createArticle(request, category, slug);
        try {
            articleRepository.saveAndFlush(article);
        } catch (DataIntegrityViolationException e) {
            // Race condition: 동시 요청으로 unique constraint 위반
            Article race = articleRepository.findBySourceUrl(request.sourceUrl())
                    .orElseThrow(() -> e);
            throw new ArticleAlreadyExistsException("Article already exists", race.getId());
        }

        // 4. Job + SubJob 3개 생성, 각 SubJob에 3개 StepJob 즉시 생성
        ArticleGenerationJob job = new ArticleGenerationJob(article.getId(), JobStatus.PENDING);
        jobRepository.save(job);

        for (DifficultyLevel level : List.of(DifficultyLevel.EASY, DifficultyLevel.MEDIUM, DifficultyLevel.HARD)) {
            ArticleGenerationSubJob subJob = new ArticleGenerationSubJob(job, level, JobStatus.PENDING);
            subJobRepository.save(subJob);
            for (GenerationStepType stepType : GenerationStepType.values()) {
                stepJobRepository.save(ArticleGenerationStepJob.pending(subJob, stepType));
            }
        }

        recordIngestionMetric("registered", System.currentTimeMillis() - startTimeMs);
        return new RegisterArticleResponse(article.getId(), job.getId(), JobStatus.PENDING.name());
    }

    private void recordIngestionMetric(String status, long durationMs) {
        if (meterRegistry != null) {
            Counter.builder("curiofeed.ingestion.articles")
                    .tag("status", status)
                    .register(meterRegistry)
                    .increment();
            Timer.builder("curiofeed.ingestion.duration")
                    .tag("status", status)
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);
        }
    }

    private Article createArticle(RegisterArticleRequest req, Category category, String slug) {
        return Article.create(
                req.originalTitle(),
                req.sourceName(),
                req.sourceUrl(),
                req.originalContent(),
                req.originalPublishedAt(),
                category,
                slug
        );
    }

    // ── Inner exception ──────────────────────────────────────────────────────

    public static class ArticleAlreadyExistsException extends RuntimeException {
        private final UUID articleId;

        public ArticleAlreadyExistsException(String message, UUID articleId) {
            super(message);
            this.articleId = articleId;
        }

        public UUID getArticleId() {
            return articleId;
        }
    }
}
