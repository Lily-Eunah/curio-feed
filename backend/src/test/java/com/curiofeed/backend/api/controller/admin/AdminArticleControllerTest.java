package com.curiofeed.backend.api.controller.admin;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class AdminArticleControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleGenerationJobRepository jobRepository;

    @Autowired
    private ArticleGenerationSubJobRepository subJobRepository;

    @Autowired
    private ArticleGenerationStepJobRepository stepJobRepository;

    @Autowired
    private EntityManager em;

    private Category category;

    @BeforeEach
    void setUp() {
        // Clean seeded data so count-based assertions are not affected by Flyway seed data
        em.createQuery("DELETE FROM Quiz").executeUpdate();
        em.createQuery("DELETE FROM Vocabulary").executeUpdate();
        em.createQuery("DELETE FROM ArticleContent").executeUpdate();
        em.createQuery("DELETE FROM ArticleGenerationStepJob").executeUpdate();
        em.createQuery("DELETE FROM ArticleGenerationSubJob").executeUpdate();
        em.createQuery("DELETE FROM ArticleGenerationJob").executeUpdate();
        em.createQuery("DELETE FROM Article").executeUpdate();
        em.createQuery("DELETE FROM Category").executeUpdate();
        category = newInstance(Category.class);
        setField(category, "name", "tech-" + UUID.randomUUID());
        setField(category, "displayName", "Technology");
        setField(category, "active", true);
        setField(category, "sortOrder", 1);
        em.persist(category);
        em.flush();
    }

    private String registerArticleJson(String sourceUrl) {
        return """
                {
                  "originalTitle": "AI is changing the world",
                  "sourceName": "TechNews",
                  "sourceUrl": "%s",
                  "originalContent": "This is the full original article content about AI.",
                  "originalPublishedAt": "2026-04-16T09:00:00Z",
                  "categoryId": "%s",
                  "thumbnailUrl": "https://example.com/thumb.jpg"
                }
                """.formatted(sourceUrl, category.getId());
    }

    @Test
    @DisplayName("POST /api/admin/articles - 정상 등록: Article, Job, SubJob 3개 DB 저장 확인")
    void registerArticle_success() throws Exception {
        String url = "https://example.com/article-" + UUID.randomUUID();

        mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articleId").isNotEmpty())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));

        em.flush(); em.clear();

        List<Article> articles = articleRepository.findAll();
        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).getStatus()).isEqualTo(ArticleStatus.DRAFT);

        List<ArticleGenerationSubJob> subJobs = subJobRepository.findAll();
        assertThat(subJobs).hasSize(3);
        assertThat(subJobs).allMatch(s -> s.getStatus() == JobStatus.PENDING);

        // Each sub-job should have exactly 3 step jobs pre-created (CONTENT, VOCABULARY, QUIZ)
        List<ArticleGenerationStepJob> stepJobs = stepJobRepository.findAll();
        assertThat(stepJobs).hasSize(9); // 3 sub-jobs × 3 steps
        assertThat(stepJobs).allMatch(s -> s.getStatus() == JobStatus.PENDING);
        assertThat(stepJobs.stream().map(ArticleGenerationStepJob::getStepType).toList())
                .containsExactlyInAnyOrder(
                        GenerationStepType.CONTENT, GenerationStepType.VOCABULARY, GenerationStepType.QUIZ,
                        GenerationStepType.CONTENT, GenerationStepType.VOCABULARY, GenerationStepType.QUIZ,
                        GenerationStepType.CONTENT, GenerationStepType.VOCABULARY, GenerationStepType.QUIZ);
    }

    @Test
    @DisplayName("POST /api/admin/articles - 중복 sourceUrl: 409 응답")
    void registerArticle_duplicateSourceUrl_conflict() throws Exception {
        String url = "https://example.com/duplicate-" + UUID.randomUUID();

        mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url)))
                .andExpect(status().isCreated());

        em.flush(); em.clear();

        mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Article already exists"))
                .andExpect(jsonPath("$.articleId").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/admin/articles/{articleId}/generation-status - 상태 조회 응답 구조 확인")
    void getGenerationStatus_success() throws Exception {
        String url = "https://example.com/status-test-" + UUID.randomUUID();
        String createResponse = mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        em.flush(); em.clear();

        // Extract articleId from response
        String articleId = createResponse.replaceAll(".*\"articleId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/admin/articles/{articleId}/generation-status", articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleId").value(articleId))
                .andExpect(jsonPath("$.articleStatus").value("DRAFT"))
                .andExpect(jsonPath("$.job.subJobs.length()").value(3))
                .andExpect(jsonPath("$.job.subJobs[0].subJobId").isNotEmpty())
                .andExpect(jsonPath("$.job.subJobs[0].level").isNotEmpty())
                .andExpect(jsonPath("$.job.subJobs[0].status").value("PENDING"))
                .andExpect(jsonPath("$.job.subJobs[0].retryCount").value(0));
    }

    @Test
    @DisplayName("GET /api/admin/articles/{articleId}/generation-status - 존재하지 않는 articleId: 404")
    void getGenerationStatus_notFound() throws Exception {
        mockMvc.perform(get("/api/admin/articles/{articleId}/generation-status", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST retry - FAILED SubJob → PENDING, retryCount=0")
    void retrySubJob_failedSubJob_resetsToPending() throws Exception {
        String url = "https://example.com/retry-test-" + UUID.randomUUID();
        String createResponse = mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        em.flush(); em.clear();

        String articleId = createResponse.replaceAll(".*\"articleId\":\"([^\"]+)\".*", "$1");
        String jobId = createResponse.replaceAll(".*\"jobId\":\"([^\"]+)\".*", "$1");

        // FAILED 상태로 강제 변경
        ArticleGenerationSubJob easySubJob = subJobRepository.findAll().stream()
                .filter(s -> s.getLevel() == DifficultyLevel.EASY)
                .findFirst().orElseThrow();
        subJobRepository.forceSetStatus(easySubJob.getId(), JobStatus.FAILED);
        em.flush(); em.clear();

        mockMvc.perform(post("/api/admin/articles/{articleId}/generation-jobs/{jobId}/sub-jobs/{subJobId}/retry",
                        articleId, jobId, easySubJob.getId()))
                .andExpect(status().isNoContent());

        em.flush(); em.clear();

        ArticleGenerationSubJob updated = subJobRepository.findById(easySubJob.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(updated.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("POST retry - FAILED가 아닌 SubJob: 400")
    void retrySubJob_notFailed_badRequest() throws Exception {
        String url = "https://example.com/retry-bad-" + UUID.randomUUID();
        String createResponse = mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        em.flush(); em.clear();

        String articleId = createResponse.replaceAll(".*\"articleId\":\"([^\"]+)\".*", "$1");
        String jobId = createResponse.replaceAll(".*\"jobId\":\"([^\"]+)\".*", "$1");

        ArticleGenerationSubJob pendingSubJob = subJobRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/api/admin/articles/{articleId}/generation-jobs/{jobId}/sub-jobs/{subJobId}/retry",
                        articleId, jobId, pendingSubJob.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("SubJob is not in FAILED state"));
    }

    // ── List Articles Tests ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/articles - 빈 목록: 페이지 응답 content 비어있음")
    void listArticles_empty() throws Exception {
        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/admin/articles - 등록된 기사 1건 조회")
    void listArticles_returnsRegisteredArticle() throws Exception {
        String url = "https://example.com/list-test-" + UUID.randomUUID();
        mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url)))
                .andExpect(status().isCreated());

        em.flush(); em.clear();

        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].originalTitle").value("AI is changing the world"))
                .andExpect(jsonPath("$.content[0].sourceName").value("TechNews"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Technology"))
                .andExpect(jsonPath("$.content[0].id").isNotEmpty())
                .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/articles?status=DRAFT - status 필터링")
    void listArticles_filterByStatus() throws Exception {
        String url = "https://example.com/status-filter-" + UUID.randomUUID();
        mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url)))
                .andExpect(status().isCreated());

        em.flush(); em.clear();

        // DRAFT status should return the article
        mockMvc.perform(get("/api/admin/articles").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        // PUBLISHED status should return empty
        mockMvc.perform(get("/api/admin/articles").param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/admin/articles?page=0&size=1 - 페이지 크기 제한")
    void listArticles_pagination() throws Exception {
        String url1 = "https://example.com/page-1-" + UUID.randomUUID();
        String url2 = "https://example.com/page-2-" + UUID.randomUUID();
        mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url1)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url2)))
                .andExpect(status().isCreated());

        em.flush(); em.clear();

        mockMvc.perform(get("/api/admin/articles")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    // ── Update Status Tests ──────────────────────────────────────────────

    private String createArticleAndGetId() throws Exception {
        String url = "https://example.com/status-" + UUID.randomUUID();
        String response = mockMvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerArticleJson(url)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        em.flush(); em.clear();
        return response.replaceAll(".*\"articleId\":\"([^\"]+)\".*", "$1");
    }

    private String statusJson(String status) {
        return "{\"status\":\"" + status + "\"}";
    }

    @Test
    @DisplayName("PATCH /api/admin/articles/{id}/status - DRAFT → PUBLISHED 성공")
    void updateStatus_draftToPublished() throws Exception {
        String articleId = createArticleAndGetId();

        mockMvc.perform(patch("/api/admin/articles/{id}/status", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("PUBLISHED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.articleId").isNotEmpty());

        em.flush(); em.clear();

        // Verify persisted
        var article = articleRepository.findById(UUID.fromString(articleId)).orElseThrow();
        assertThat(article.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
    }

    @Test
    @DisplayName("PATCH /api/admin/articles/{id}/status - PUBLISHED → HIDDEN 성공")
    void updateStatus_publishedToHidden() throws Exception {
        String articleId = createArticleAndGetId();

        // First: DRAFT → PUBLISHED
        mockMvc.perform(patch("/api/admin/articles/{id}/status", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("PUBLISHED")))
                .andExpect(status().isOk());
        em.flush(); em.clear();

        // Then: PUBLISHED → HIDDEN
        mockMvc.perform(patch("/api/admin/articles/{id}/status", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("HIDDEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));
    }

    @Test
    @DisplayName("PATCH /api/admin/articles/{id}/status - HIDDEN → PUBLISHED 복원")
    void updateStatus_hiddenToPublished() throws Exception {
        String articleId = createArticleAndGetId();

        // DRAFT → PUBLISHED → HIDDEN
        mockMvc.perform(patch("/api/admin/articles/{id}/status", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("PUBLISHED")))
                .andExpect(status().isOk());
        em.flush(); em.clear();

        mockMvc.perform(patch("/api/admin/articles/{id}/status", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("HIDDEN")))
                .andExpect(status().isOk());
        em.flush(); em.clear();

        // HIDDEN → PUBLISHED
        mockMvc.perform(patch("/api/admin/articles/{id}/status", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("PUBLISHED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("PATCH /api/admin/articles/{id}/status - 허용되지 않는 전환 (DRAFT → HIDDEN): 400")
    void updateStatus_invalidTransition() throws Exception {
        String articleId = createArticleAndGetId();

        mockMvc.perform(patch("/api/admin/articles/{id}/status", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("HIDDEN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid status transition")));
    }

    @Test
    @DisplayName("PATCH /api/admin/articles/{id}/status - 존재하지 않는 article: 404")
    void updateStatus_notFound() throws Exception {
        mockMvc.perform(patch("/api/admin/articles/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson("PUBLISHED")))
                .andExpect(status().isNotFound());
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
