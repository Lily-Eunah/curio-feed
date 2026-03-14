package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.api.dto.ArticleFeedResponse;
import com.curiofeed.backend.domain.entity.Article;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.entity.Category;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(com.curiofeed.backend.config.JpaConfig.class)
class ArticleFeedRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ArticleFeedRepository feedRepository;

    @Autowired
    private EntityManager em;

    private Category category;

    // ── Helper: Reflection-based entity builder ─────────

    private Category createCategory(String name, String displayName) {
        Category cat = newInstance(Category.class);
        setField(cat, "name", name);
        setField(cat, "displayName", displayName);
        setField(cat, "active", true);
        setField(cat, "sortOrder", 1);
        em.persist(cat);
        return cat;
    }

    private Article createArticle(Category cat, ArticleStatus status, Instant publishedAt, String slug) {
        Article article = newInstance(Article.class);
        setField(article, "originalTitle", "Original: " + slug);
        setField(article, "sourceName", "Test Source");
        setField(article, "sourceUrl", "https://example.com/" + slug);
        setField(article, "originalPublishedAt", publishedAt);
        setField(article, "title", "Title: " + slug);
        setField(article, "slug", slug);
        setField(article, "category", cat);
        setField(article, "publishedAt", publishedAt);
        setField(article, "status", status);
        setField(article, "thumbnailUrl", "https://example.com/thumb/" + slug + ".jpg");
        em.persist(article);
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

    // ── Setup ───────────────────────────────────────────

    @BeforeEach
    void setUp() {
        category = createCategory("tech", "Technology");

        Instant base = Instant.parse("2026-03-01T00:00:00Z");

        // PUBLISHED articles (5) — each 1 hour apart
        for (int i = 0; i < 5; i++) {
            createArticle(category, ArticleStatus.PUBLISHED,
                    base.minusSeconds(i * 3600L), "published-" + i);
        }

        // DRAFT articles (2)
        createArticle(category, ArticleStatus.DRAFT,
                base.minusSeconds(6 * 3600L), "draft-1");
        createArticle(category, ArticleStatus.DRAFT,
                base.minusSeconds(7 * 3600L), "draft-2");

        // HIDDEN article (1)
        createArticle(category, ArticleStatus.HIDDEN,
                base.minusSeconds(8 * 3600L), "hidden-1");

        em.flush();
        em.clear();
    }

    // ── Tests ───────────────────────────────────────────

    @Test
    @DisplayName("PUBLISHED 상태의 기사만 조회한다")
    void shouldReturnOnlyPublishedArticles() {
        List<ArticleFeedResponse> results = feedRepository.findFeedFirstPage(
                ArticleStatus.PUBLISHED, PageRequest.of(0, 100));

        assertThat(results).hasSize(5);
    }

    @Test
    @DisplayName("publishedAt DESC, id DESC 순서로 정렬된다")
    void shouldOrderByPublishedAtDescThenIdDesc() {
        List<ArticleFeedResponse> results = feedRepository.findFeedFirstPage(
                ArticleStatus.PUBLISHED, PageRequest.of(0, 100));

        for (int i = 0; i < results.size() - 1; i++) {
            Instant current = results.get(i).getPublishedAt();
            Instant next = results.get(i + 1).getPublishedAt();
            assertThat(current).isAfterOrEqualTo(next);
        }
    }

    @Test
    @DisplayName("요청한 size만큼만 반환한다")
    void shouldRespectPageSize() {
        List<ArticleFeedResponse> results = feedRepository.findFeedFirstPage(
                ArticleStatus.PUBLISHED, PageRequest.of(0, 3));

        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("커서 기준 이전 데이터만 반환한다")
    void shouldReturnArticlesBeforeCursor() {
        // Get first page to extract cursor
        List<ArticleFeedResponse> firstPage = feedRepository.findFeedFirstPage(
                ArticleStatus.PUBLISHED, PageRequest.of(0, 2));

        ArticleFeedResponse lastItem = firstPage.get(firstPage.size() - 1);
        Instant cursorAt = lastItem.getPublishedAt();
        UUID cursorId = UUID.fromString(lastItem.getId());

        // Get second page via cursor
        List<ArticleFeedResponse> secondPage = feedRepository.findFeedByCursor(
                ArticleStatus.PUBLISHED, cursorAt, cursorId, PageRequest.of(0, 100));

        assertThat(secondPage).isNotEmpty();
        for (ArticleFeedResponse item : secondPage) {
            boolean isBeforeCursor =
                    item.getPublishedAt().isBefore(cursorAt) ||
                    (item.getPublishedAt().equals(cursorAt) &&
                     UUID.fromString(item.getId()).compareTo(cursorId) < 0);
            assertThat(isBeforeCursor).isTrue();
        }
    }

    @Test
    @DisplayName("동일 publishedAt에서 id tie-breaker가 정확히 작동한다")
    void shouldHandleSamePublishedAtWithIdTieBreaker() {
        Instant sameTime = Instant.parse("2026-06-01T00:00:00Z");
        Article a1 = createArticle(category, ArticleStatus.PUBLISHED, sameTime, "same-time-1");
        Article a2 = createArticle(category, ArticleStatus.PUBLISHED, sameTime, "same-time-2");
        em.flush();
        em.clear();

        List<ArticleFeedResponse> results = feedRepository.findFeedFirstPage(
                ArticleStatus.PUBLISHED, PageRequest.of(0, 100));

        // Find the two same-time results
        List<ArticleFeedResponse> sameTimeResults = results.stream()
                .filter(r -> r.getPublishedAt().equals(sameTime))
                .toList();

        assertThat(sameTimeResults).hasSize(2);
        // First should have larger UUID (DESC order)
        UUID firstId = UUID.fromString(sameTimeResults.get(0).getId());
        UUID secondId = UUID.fromString(sameTimeResults.get(1).getId());
        assertThat(firstId).isGreaterThan(secondId);
    }

    @Test
    @DisplayName("동일 publishedAt에서 cursor id 이하의 기사만 정확히 반환한다")
    void shouldReturnExactRowsBelowCursorId_whenSamePublishedAt() {
        Instant sameTime = Instant.parse("2026-06-15T12:00:00Z");
        Article a1 = createArticle(category, ArticleStatus.PUBLISHED, sameTime, "boundary-1");
        Article a2 = createArticle(category, ArticleStatus.PUBLISHED, sameTime, "boundary-2");
        Article a3 = createArticle(category, ArticleStatus.PUBLISHED, sameTime, "boundary-3");
        em.flush();
        em.clear();

        // Sort IDs descending to find the "middle" one as cursor
        List<UUID> sortedIds = List.of(a1.getId(), a2.getId(), a3.getId()).stream()
                .sorted(java.util.Comparator.reverseOrder())
                .toList();
        UUID cursorId = sortedIds.get(0); // largest id (top of the list)

        List<ArticleFeedResponse> results = feedRepository.findFeedByCursor(
                ArticleStatus.PUBLISHED, sameTime, cursorId, PageRequest.of(0, 100));

        // Should return the other sameTime articles + earlier articles, but NOT the cursor itself
        List<String> returnedIds = results.stream()
                .map(ArticleFeedResponse::getId)
                .toList();
        assertThat(returnedIds).doesNotContain(cursorId.toString());

        // All sameTime results should have id < cursorId
        results.stream()
                .filter(r -> r.getPublishedAt().equals(sameTime))
                .forEach(r -> assertThat(UUID.fromString(r.getId())).isLessThan(cursorId));
    }

    @Test
    @DisplayName("Category JOIN으로 displayName을 정확히 반환한다")
    void shouldJoinCategoryAndReturnDisplayName() {
        List<ArticleFeedResponse> results = feedRepository.findFeedFirstPage(
                ArticleStatus.PUBLISHED, PageRequest.of(0, 1));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategoryName()).isEqualTo("Technology");
    }

    @Test
    @DisplayName("PUBLISHED 기사가 없으면 빈 리스트를 반환한다")
    void shouldReturnEmptyList_whenNoPublishedArticles() {
        // Clear all existing and create only non-published
        em.createQuery("DELETE FROM Article").executeUpdate();
        createArticle(category, ArticleStatus.DRAFT,
                Instant.parse("2026-01-01T00:00:00Z"), "only-draft");
        em.flush();
        em.clear();

        List<ArticleFeedResponse> results = feedRepository.findFeedFirstPage(
                ArticleStatus.PUBLISHED, PageRequest.of(0, 100));

        assertThat(results).isEmpty();
    }
}
