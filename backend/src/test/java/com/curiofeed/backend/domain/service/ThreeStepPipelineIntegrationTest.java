package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import com.curiofeed.backend.infrastructure.llm.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class ThreeStepPipelineIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ThreeStepPipelineIntegrationTest.class);

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

    @MockBean
    @Qualifier("primaryLlmClient")
    private LlmClient mockLlmClient;

    @MockBean
    @Qualifier("fallbackLlmClient")
    private LlmClient mockFallbackLlmClient;

    private Category category;
    private Article article;
    private ArticleGenerationJob job;
    private ArticleGenerationSubJob subJob;

    private boolean useLiveLlm = "true".equalsIgnoreCase(System.getenv("USE_LIVE_LLM"));

    @BeforeEach
    void setUp() {
        String uniqueName = "Test-" + UUID.randomUUID();
        category = categoryRepository.saveAndFlush(new Category(uniqueName, uniqueName, 1, true));
        
        article = articleRepository.saveAndFlush(Article.create(
                "Why AI is changing the world " + UUID.randomUUID(),
                "TechNews",
                "http://example.com/ai-news-" + UUID.randomUUID(),
                "Artificial intelligence is transforming every industry. From healthcare to finance, " +
                "machine learning models are being used to automate complex tasks and provide deep insights. " +
                "However, this transformation also brings challenges related to ethics, privacy, and job displacement. " +
                "In this article, we explore the current state of AI and its future implications.",
                Instant.now(),
                category,
                "ai-news-" + UUID.randomUUID()
        ));
        job = jobRepository.saveAndFlush(new ArticleGenerationJob(article.getId(), JobStatus.PROCESSING));
        subJob = subJobRepository.saveAndFlush(new ArticleGenerationSubJob(job, DifficultyLevel.MEDIUM, JobStatus.PENDING));

        if (!useLiveLlm) {
            setupMockLlm();
        }
    }

    private void setupMockLlm() {
        StringBuilder longContent = new StringBuilder("AI is changing everything. transforming automate insights ethics displacement ");
        for (int i = 0; i < 200; i++) {
            longContent.append("Word" + i + " ");
        }
        String contentJson = "{\"content\": \"" + longContent.toString().trim() + "\"}";
        String vocabJson = "{\"vocabularies\": [" +
                "{\"word\": \"transforming\", \"definition\": \"changing completely. used when life changes.\", \"exampleSentence\": \"AI is transforming life.\"}," +
                "{\"word\": \"automate\", \"definition\": \"make automatic. used when we use machines.\", \"exampleSentence\": \"We automate tasks.\"}," +
                "{\"word\": \"insights\", \"definition\": \"deep understanding. used when we learn deeply.\", \"exampleSentence\": \"We need insights.\"}," +
                "{\"word\": \"ethics\", \"definition\": \"moral principles. used when we talk about right or wrong.\", \"exampleSentence\": \"Ethics are important.\"}," +
                "{\"word\": \"displacement\", \"definition\": \"moving something. used when jobs move.\", \"exampleSentence\": \"Job displacement is real.\"} " +
                "]}";
        String quizJson = "{\"quizzes\": [" +
                "{\"type\": \"MULTIPLE_CHOICE\", \"question\": \"What is AI doing?\", \"options\": {\"choices\": [{\"key\": \"A\", \"text\": \"Transforming life\", \"explanation\": \"Yes\"}]}, \"correctAnswer\": \"A\", \"explanation\": \"Reason\"}," +
                "{\"type\": \"MULTIPLE_CHOICE\", \"question\": \"Word for changing?\", \"options\": {\"choices\": [{\"key\": \"A\", \"text\": \"transforming\", \"explanation\": \"Yes\"}]}, \"correctAnswer\": \"A\", \"explanation\": \"Reason\"}," +
                "{\"type\": \"SHORT_ANSWER\", \"question\": \"What are important?\", \"options\": {}, \"correctAnswer\": \"ethics\", \"explanation\": \"Reason\"}" +
                "]}";

        when(mockLlmClient.generate(anyString(), anyMap()))
                .thenReturn(contentJson, contentJson, contentJson) // Support retries if needed
                .thenReturn(vocabJson, vocabJson, vocabJson)
                .thenReturn(quizJson, quizJson, quizJson);

        when(mockFallbackLlmClient.generate(anyString(), anyMap()))
                .thenReturn(contentJson, contentJson, contentJson)
                .thenReturn(vocabJson, vocabJson, vocabJson)
                .thenReturn(quizJson, quizJson, quizJson);
    }

    @Test
    @Tag("pending")
    void process_fullExecution_reportsDurations() throws InterruptedException {
        log.info("Starting integration test. UseLiveLlm={}", useLiveLlm);
        Instant start = Instant.now();

        // Execute
        worker.process(subJob.getId());

        // Wait for async completion
        waitForStatus(subJob.getId(), List.of(JobStatus.COMPLETED, JobStatus.FAILED), 30);

        Instant end = Instant.now();
        log.info("[test-metrics] Total Duration: {}ms", end.toEpochMilli() - start.toEpochMilli());

        // Verify terminal states
        ArticleGenerationSubJob updatedSubJob = subJobRepository.findById(subJob.getId()).orElseThrow();
        assertThat(updatedSubJob.getStatus()).isEqualTo(JobStatus.COMPLETED);

        List<ArticleGenerationStepJob> steps = stepJobRepository.findBySubJobIdOrderByStepType(subJob.getId());
        // Should be 4 (DIGEST, CONTENT, VOCAB, QUIZ)
        assertThat(steps.size()).isEqualTo(4);
        
        ArticleGenerationStepJob digestStep = steps.stream()
                .filter(s -> s.getStepType() == GenerationStepType.SOURCE_DIGEST)
                .findFirst().orElseThrow();
        assertThat(digestStep.getStatus()).isEqualTo(JobStatus.SKIPPED);
        assertThat(digestStep.getValidationStatus()).isEqualTo("SKIPPED");
        assertThat(digestStep.getErrorMessage()).isEqualTo("SHORT_ARTICLE");

        for (var s : steps) {
            if (s.getStepType() == GenerationStepType.SOURCE_DIGEST) continue;
            assertThat(s.getStatus()).isEqualTo(JobStatus.COMPLETED);
            long stepDuration = (s.getCompletedAt() != null && s.getStartedAt() != null) 
                ? s.getCompletedAt().toEpochMilli() - s.getStartedAt().toEpochMilli() : -1;
            log.info("[test-metrics] Step: {} Duration: {}ms", s.getStepType(), stepDuration);
        }

        // Verify Data
        assertThat(contentRepository.findByArticleIdAndLevel(article.getId(), DifficultyLevel.MEDIUM)).isPresent();
    }

    @Test
    void process_longArticle_triggersSourceDigest() throws InterruptedException {
        if (useLiveLlm) return;

        // Create a long article (777 words)
        String longContent = "Word ".repeat(777);
        Article longArticle = articleRepository.saveAndFlush(Article.create(
                "Long News " + UUID.randomUUID(),
                "Tech",
                "http://example.com/long-" + UUID.randomUUID(),
                longContent,
                Instant.now(),
                category,
                "long-" + UUID.randomUUID()
        ));
        ArticleGenerationJob longJob = jobRepository.saveAndFlush(new ArticleGenerationJob(longArticle.getId(), JobStatus.PROCESSING));
        ArticleGenerationSubJob longSubJob = subJobRepository.saveAndFlush(new ArticleGenerationSubJob(longJob, DifficultyLevel.HARD, JobStatus.PENDING));

        // Mock LLM to return Digest then Content etc.
        String digestJson = "{\"sourceDigest\": {\"centralStory\": \"AI is changing everything.\", \"coreFacts\": [\"Fact 1\"], \"supportingDetails\": [], \"omittedDetails\": []}}";
        String contentJson = "{\"content\": \"" + "AI is transforming the world in many ways. ".repeat(40) + "This is a hard level article.\"}";
        String vocabJson = "{\"vocabularies\": [" +
                "{\"word\": \"transforming\", \"definition\": \"changing. used when...\", \"exampleSentence\": \"AI is transforming life.\"}," +
                "{\"word\": \"world\", \"definition\": \"planet. used when...\", \"exampleSentence\": \"The world is big.\"}," +
                "{\"word\": \"ways\", \"definition\": \"methods. used when...\", \"exampleSentence\": \"There are many ways.\"}," +
                "{\"word\": \"hard\", \"definition\": \"difficult. used when...\", \"exampleSentence\": \"This is hard.\"}," +
                "{\"word\": \"article\", \"definition\": \"text. used when...\", \"exampleSentence\": \"Read this article.\"} " +
                "]}";
        String quizJson = "{\"quizzes\": [" +
                "{\"type\": \"MULTIPLE_CHOICE\", \"question\": \"Q1\", \"options\": {\"choices\": [{\"key\": \"A\", \"text\": \"T\", \"explanation\": \"E\"}]}, \"correctAnswer\": \"A\", \"explanation\": \"R\"}," +
                "{\"type\": \"MULTIPLE_CHOICE\", \"question\": \"Q2\", \"options\": {\"choices\": [{\"key\": \"A\", \"text\": \"world\", \"explanation\": \"E\"}]}, \"correctAnswer\": \"A\", \"explanation\": \"R\"}," +
                "{\"type\": \"SHORT_ANSWER\", \"question\": \"Q3\", \"options\": {}, \"correctAnswer\": \"hard\", \"explanation\": \"R\"}" +
                "]}";

        when(mockLlmClient.generate(anyString(), anyMap()))
                .thenReturn(digestJson)
                .thenReturn(contentJson)
                .thenReturn(vocabJson)
                .thenReturn(quizJson);

        // Execute
        worker.process(longSubJob.getId());

        // Wait
        waitForStatus(longSubJob.getId(), List.of(JobStatus.COMPLETED, JobStatus.FAILED), 20);

        // Verify
        ArticleGenerationSubJob updated = subJobRepository.findById(longSubJob.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.COMPLETED);

        ArticleGenerationStepJob digestStep = stepJobRepository.findBySubJobIdAndStepType(longSubJob.getId(), GenerationStepType.SOURCE_DIGEST).orElseThrow();
        assertThat(digestStep.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(digestStep.getValidationStatus()).isEqualTo("PASS");
    }

    @Test
    @Tag("pending")
    void process_llmFailure_transitionsToFailed() throws InterruptedException {
        if (useLiveLlm) return; // Only for mock mode

        when(mockLlmClient.generate(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Simulated LLM Connection Timeout"));
        when(mockFallbackLlmClient.generate(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Simulated LLM Connection Timeout"));

        worker.process(subJob.getId());
        waitForStatus(subJob.getId(), List.of(JobStatus.FAILED, JobStatus.COMPLETED), 10);

        ArticleGenerationSubJob updatedSubJob = subJobRepository.findById(subJob.getId()).orElseThrow();
        assertThat(updatedSubJob.getStatus()).isEqualTo(JobStatus.FAILED);

        List<ArticleGenerationStepJob> steps = stepJobRepository.findBySubJobIdOrderByStepType(subJob.getId());
        assertThat(steps.stream().anyMatch(s -> s.getStatus() == JobStatus.FAILED)).isTrue();
        
        var failedStep = steps.stream().filter(s -> s.getStatus() == JobStatus.FAILED).findFirst().get();
        log.info("[test-metrics] Failed Step Reason: {}", failedStep.getFailureReason());
        log.info("[test-metrics] Failed Step Error: {}", failedStep.getErrorMessage());
        
        assertThat(failedStep.getFailureReason()).isNotNull();
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
