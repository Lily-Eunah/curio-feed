package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.admin.RegisterArticleRequest;
import com.curiofeed.backend.api.dto.admin.RegisterArticleResponse;
import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ArticleIngestionService {

    private final ArticleRepository articleRepository;
    private final ArticleGenerationJobRepository jobRepository;
    private final ArticleGenerationSubJobRepository subJobRepository;
    private final CategoryRepository categoryRepository;
    private final SlugGenerator slugGenerator;

    public ArticleIngestionService(
            ArticleRepository articleRepository,
            ArticleGenerationJobRepository jobRepository,
            ArticleGenerationSubJobRepository subJobRepository,
            CategoryRepository categoryRepository,
            SlugGenerator slugGenerator) {
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
        this.subJobRepository = subJobRepository;
        this.categoryRepository = categoryRepository;
        this.slugGenerator = slugGenerator;
    }

    /**
     * 기사 등록 + Job 생성.
     * sourceUrl 기반 idempotency: 이미 존재하면 ConflictException throw.
     */
    @Transactional
    public RegisterArticleResponse register(RegisterArticleRequest request) {
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

        // 4. Job + SubJob 3개 생성
        ArticleGenerationJob job = new ArticleGenerationJob(article.getId(), JobStatus.PENDING);
        jobRepository.save(job);

        for (DifficultyLevel level : List.of(DifficultyLevel.EASY, DifficultyLevel.MEDIUM, DifficultyLevel.HARD)) {
            subJobRepository.save(new ArticleGenerationSubJob(job, level, JobStatus.PENDING));
        }

        return new RegisterArticleResponse(article.getId(), job.getId(), JobStatus.PENDING.name());
    }

    private Article createArticle(RegisterArticleRequest req, Category category, String slug) {
        Article article = newInstance(Article.class);
        setField(article, "originalTitle", req.originalTitle());
        setField(article, "sourceName", req.sourceName());
        setField(article, "sourceUrl", req.sourceUrl());
        setField(article, "originalContent", req.originalContent());
        setField(article, "originalPublishedAt", req.originalPublishedAt());
        setField(article, "title", req.originalTitle());
        setField(article, "slug", slug);
        setField(article, "category", category);
        setField(article, "publishedAt", req.originalPublishedAt());
        setField(article, "status", ArticleStatus.DRAFT);
        setField(article, "thumbnailUrl", req.thumbnailUrl());
        return article;
    }

    @SuppressWarnings("unchecked")
    private <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("Field not found: " + fieldName);
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
