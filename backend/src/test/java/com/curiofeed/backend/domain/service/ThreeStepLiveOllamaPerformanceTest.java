package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live performance test for Ollama pipeline.
 * Run with: RUN_LIVE_OLLAMA_TESTS=true ./gradlew test --tests ThreeStepLiveOllamaPerformanceTest
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_OLLAMA_TESTS", matches = "true")
public class ThreeStepLiveOllamaPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(ThreeStepLiveOllamaPerformanceTest.class);

    @Autowired
    private ThreeStepSubJobWorker worker;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleGenerationJobRepository jobRepository;

    @Autowired
    private ArticleGenerationSubJobRepository subJobRepository;

    @Autowired
    private ArticleGenerationStepJobRepository stepJobRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ArticleContentRepository contentRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        String uniqueName = "LiveTest-" + UUID.randomUUID();
        category = categoryRepository.saveAndFlush(new Category(uniqueName, uniqueName, 1, true));
    }

    @Test
    void measurePerformance_longArticle_HardLevel() throws InterruptedException {
        // ~780 words article to trigger SOURCE_DIGEST
        String originalContent = "Artificial intelligence is rapidly evolving. ".repeat(156) + "This is a long article about AI impact.";
        Article article = createArticle("AI Impact " + UUID.randomUUID(), originalContent);
        
        ArticleGenerationJob job = jobRepository.saveAndFlush(new ArticleGenerationJob(article.getId(), JobStatus.PROCESSING));
        ArticleGenerationSubJob subJob = subJobRepository.saveAndFlush(new ArticleGenerationSubJob(job, DifficultyLevel.HARD, JobStatus.PENDING));

        log.info("[performance-test] Starting live Ollama test for HARD level (long article)");
        Instant start = Instant.now();
        
        worker.process(subJob.getId());
        waitForStatus(subJob.getId(), List.of(JobStatus.COMPLETED, JobStatus.FAILED), 600); // 10 min timeout for live LLM

        Instant end = Instant.now();
        log.info("[performance-test] Total Duration: {}ms", end.toEpochMilli() - start.toEpochMilli());

        verifyAndReport(subJob.getId(), DifficultyLevel.HARD, article.getId());
    }

    @Test
    void measurePerformance_shortArticle_EasyLevel() throws InterruptedException {
        // 200 words article
        String originalContent = "AI is a tool. ".repeat(40) + "It helps people do work faster.";
        Article article = createArticle("AI Tools " + UUID.randomUUID(), originalContent);
        
        ArticleGenerationJob job = jobRepository.saveAndFlush(new ArticleGenerationJob(article.getId(), JobStatus.PROCESSING));
        ArticleGenerationSubJob subJob = subJobRepository.saveAndFlush(new ArticleGenerationSubJob(job, DifficultyLevel.EASY, JobStatus.PENDING));

        log.info("[performance-test] Starting live Ollama test for EASY level (short article)");
        Instant start = Instant.now();
        
        worker.process(subJob.getId());
        waitForStatus(subJob.getId(), List.of(JobStatus.COMPLETED, JobStatus.FAILED), 300); // 5 min timeout

        Instant end = Instant.now();
        log.info("[performance-test] Total Duration: {}ms", end.toEpochMilli() - start.toEpochMilli());

        verifyAndReport(subJob.getId(), DifficultyLevel.EASY, article.getId());
    }

    private Article createArticle(String title, String content) {
        return articleRepository.saveAndFlush(Article.create(
                title, "Tech", "http://example.com/" + UUID.randomUUID(),
                content, Instant.now(), category, "slug-" + UUID.randomUUID()
        ));
    }

    private void verifyAndReport(UUID subJobId, DifficultyLevel level, UUID articleId) {
        ArticleGenerationSubJob subJob = subJobRepository.findById(subJobId).orElseThrow();
        assertThat(subJob.getStatus()).isEqualTo(JobStatus.COMPLETED);

        List<ArticleGenerationStepJob> steps = stepJobRepository.findBySubJobIdOrderByStepType(subJobId);
        log.info("[performance-report] Level: {}", level);
        for (var s : steps) {
            long duration = (s.getCompletedAt() != null && s.getStartedAt() != null)
                    ? s.getCompletedAt().toEpochMilli() - s.getStartedAt().toEpochMilli() : -1;
            log.info("[performance-report] Step: {} Status: {} Duration: {}ms Validation: {}", 
                    s.getStepType(), s.getStatus(), duration, s.getValidationStatus());
        }

        contentRepository.findByArticleIdAndLevel(articleId, level).ifPresent(c -> {
            int wordCount = c.getContent().trim().split("\\s+").length;
            log.info("[performance-report] Generated Content Word Count: {}", wordCount);
        });
    }

    private void waitForStatus(UUID subJobId, List<JobStatus> terminalStatuses, int timeoutSeconds) throws InterruptedException {
        for (int i = 0; i < timeoutSeconds * 2; i++) {
            ArticleGenerationSubJob s = subJobRepository.findById(subJobId).orElseThrow();
            if (terminalStatuses.contains(s.getStatus())) return;
            Thread.sleep(500);
        }
        throw new RuntimeException("Timeout waiting for subjob status: " + subJobId);
    }
}
