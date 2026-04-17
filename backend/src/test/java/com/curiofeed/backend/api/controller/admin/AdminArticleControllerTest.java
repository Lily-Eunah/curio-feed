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
    private EntityManager em;

    private Category category;

    @BeforeEach
    void setUp() {
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
                .andExpect(jsonPath("$.job.subJobs.length()").value(3));
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
