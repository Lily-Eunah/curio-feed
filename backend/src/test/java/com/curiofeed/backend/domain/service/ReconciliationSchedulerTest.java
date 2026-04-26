package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.config.PipelineProperties;
import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ReconciliationSchedulerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReconciliationScheduler reconciliationScheduler;

    @Autowired
    private ArticleGenerationSubJobRepository subJobRepository;

    @Autowired
    private ArticleGenerationJobRepository jobRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private EntityManager em;

    private Article article;
    private ArticleGenerationJob job;

    @BeforeEach
    void setUp() {
        Category category = newInstance(Category.class);
        setField(category, "name", "cat-" + UUID.randomUUID());
        setField(category, "displayName", "Cat");
        setField(category, "active", true);
        setField(category, "sortOrder", 1);
        em.persist(category);

        article = newInstance(Article.class);
        setField(article, "originalTitle", "T");
        setField(article, "sourceName", "S");
        setField(article, "sourceUrl", "https://e.com/" + UUID.randomUUID());
        setField(article, "originalPublishedAt", Instant.now());
        setField(article, "title", "T");
        setField(article, "slug", "s-" + UUID.randomUUID());
        setField(article, "category", category);
        setField(article, "publishedAt", Instant.now());
        setField(article, "status", ArticleStatus.DRAFT);
        setField(article, "thumbnailUrl", "https://t.jpg");
        em.persist(article);

        job = new ArticleGenerationJob(article.getId(), JobStatus.PENDING);
        em.persist(job);
        em.flush();
    }

    private ArticleGenerationSubJob createSubJob(DifficultyLevel level, JobStatus status) {
        ArticleGenerationSubJob subJob = new ArticleGenerationSubJob(job, level, status);
        em.persist(subJob);
        return subJob;
    }

    // ── recoverStaleJobs ────────────────────────────────────────────────────

    @Test
    @DisplayName("stale PROCESSING SubJob (retryCount=0) → PENDING으로 변경")
    void recoverStaleJobs_lowRetry_setsPending() {
        ArticleGenerationSubJob subJob = createSubJob(DifficultyLevel.EASY, JobStatus.PROCESSING);
        // lastHeartbeatAt을 임계값 이전으로 설정
        Instant staleTime = Instant.now().minus(20, ChronoUnit.MINUTES);
        setField(subJob, "lastHeartbeatAt", staleTime);
        em.flush(); em.clear();

        reconciliationScheduler.recoverStaleJobs();
        em.flush(); em.clear();

        ArticleGenerationSubJob updated = subJobRepository.findById(subJob.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    @DisplayName("stale PROCESSING SubJob (retryCount=maxRetryCount) → FAILED로 변경")
    void recoverStaleJobs_maxRetry_setsFailed() {
        ArticleGenerationSubJob subJob = createSubJob(DifficultyLevel.MEDIUM, JobStatus.PROCESSING);
        Instant staleTime = Instant.now().minus(20, ChronoUnit.MINUTES);
        setField(subJob, "lastHeartbeatAt", staleTime);
        setField(subJob, "retryCount", 3); // maxRetryCount=3
        em.flush(); em.clear();

        reconciliationScheduler.recoverStaleJobs();
        em.flush(); em.clear();

        ArticleGenerationSubJob updated = subJobRepository.findById(subJob.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    @DisplayName("non-stale PROCESSING SubJob → 변경 없음")
    void recoverStaleJobs_recentHeartbeat_noChange() {
        ArticleGenerationSubJob subJob = createSubJob(DifficultyLevel.HARD, JobStatus.PROCESSING);
        // 최근 heartbeat (1분 전)
        setField(subJob, "lastHeartbeatAt", Instant.now().minus(1, ChronoUnit.MINUTES));
        em.flush(); em.clear();

        reconciliationScheduler.recoverStaleJobs();
        em.flush(); em.clear();

        ArticleGenerationSubJob updated = subJobRepository.findById(subJob.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.PROCESSING);
    }

    @Test
    @DisplayName("reconcileArticleStatus: 조건 충족 시 Article 상태 집계")
    void reconcileArticleStatus_allTerminal_aggregates() {
        createSubJob(DifficultyLevel.EASY, JobStatus.COMPLETED);
        createSubJob(DifficultyLevel.MEDIUM, JobStatus.COMPLETED);
        createSubJob(DifficultyLevel.HARD, JobStatus.FAILED);
        em.flush(); em.clear();

        reconciliationScheduler.reconcileArticleStatus();
        em.flush(); em.clear();

        Article updated = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ArticleStatus.REVIEWING);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
}
