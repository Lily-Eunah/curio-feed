package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({com.curiofeed.backend.config.JpaConfig.class, ArticleStatusAggregator.class})
class ArticleStatusAggregatorTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ArticleStatusAggregator aggregator;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleGenerationJobRepository jobRepository;

    @Autowired
    private ArticleGenerationSubJobRepository subJobRepository;

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
        setField(article, "sourceTitle", "Test");
        setField(article, "sourcePublisher", "Src");
        setField(article, "sourceUrl", "https://example.com/" + UUID.randomUUID());
        setField(article, "sourcePublishedAt", Instant.now());
        setField(article, "title", "Test");
        setField(article, "slug", "slug-" + UUID.randomUUID());
        setField(article, "category", category);
        setField(article, "publishedAt", Instant.now());
        setField(article, "status", ArticleStatus.DRAFT);
        em.persist(article);

        job = new ArticleGenerationJob(article.getId(), JobStatus.PENDING);
        em.persist(job);
        em.flush();
    }

    private void createSubJob(DifficultyLevel level, JobStatus status) {
        ArticleGenerationSubJob subJob = new ArticleGenerationSubJob(job, level, status);
        em.persist(subJob);
    }

    @Test
    @DisplayName("모든 SubJob COMPLETED → Article.status = DRAFT (관리자가 명시적으로 PUBLISHED 처리)")
    void allCompleted_setsDraft() {
        createSubJob(DifficultyLevel.EASY, JobStatus.COMPLETED);
        createSubJob(DifficultyLevel.MEDIUM, JobStatus.COMPLETED);
        createSubJob(DifficultyLevel.HARD, JobStatus.COMPLETED);
        em.flush(); em.clear();

        aggregator.aggregate(job.getId());
        em.flush(); em.clear();

        Article updated = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    @DisplayName("모든 SubJob FAILED → Article.status = DRAFT (콘텐츠 없어도 DRAFT 유지)")
    void allFailed_setsDraft() {
        createSubJob(DifficultyLevel.EASY, JobStatus.FAILED);
        createSubJob(DifficultyLevel.MEDIUM, JobStatus.FAILED);
        createSubJob(DifficultyLevel.HARD, JobStatus.FAILED);
        em.flush(); em.clear();

        aggregator.aggregate(job.getId());
        em.flush(); em.clear();

        Article updated = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    @DisplayName("부분 성공 (1 COMPLETED, 2 FAILED) → Article.status = DRAFT")
    void partial_setsDraft() {
        createSubJob(DifficultyLevel.EASY, JobStatus.COMPLETED);
        createSubJob(DifficultyLevel.MEDIUM, JobStatus.FAILED);
        createSubJob(DifficultyLevel.HARD, JobStatus.FAILED);
        em.flush(); em.clear();

        aggregator.aggregate(job.getId());
        em.flush(); em.clear();

        Article updated = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    @DisplayName("PENDING 포함 → 집계 생략, Article.status 변경 없음")
    void hasPending_skipsAggregation() {
        createSubJob(DifficultyLevel.EASY, JobStatus.COMPLETED);
        createSubJob(DifficultyLevel.MEDIUM, JobStatus.PENDING);
        createSubJob(DifficultyLevel.HARD, JobStatus.FAILED);
        em.flush(); em.clear();

        aggregator.aggregate(job.getId());
        em.flush(); em.clear();

        Article updated = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ArticleStatus.DRAFT);
    }

    // ── Reflection helpers ───────────────────────────────────────────────────

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
