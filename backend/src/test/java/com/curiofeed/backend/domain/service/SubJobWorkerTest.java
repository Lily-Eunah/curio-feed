package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.config.PipelineProperties;
import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import com.curiofeed.backend.domain.repository.ArticleRepository;
import com.curiofeed.backend.infrastructure.llm.ArticlePromptBuilder;
import com.curiofeed.backend.infrastructure.llm.LlmClient;
import com.curiofeed.backend.infrastructure.llm.LlmClientException;
import com.curiofeed.backend.infrastructure.llm.LlmParseException;
import com.curiofeed.backend.infrastructure.llm.LlmResponseParser;
import com.curiofeed.backend.infrastructure.llm.validation.GenerationResultValidator;
import com.curiofeed.backend.infrastructure.llm.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubJobWorkerTest {

    @Mock private SubJobLockService lockService;
    @Mock private ArticleGenerationSubJobRepository subJobRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private ArticlePromptBuilder promptBuilder;
    @Mock private LlmClient primaryLlmClient;
    @Mock private LlmResponseParser responseParser;
    @Mock private GenerationResultValidator validator;
    @Mock private GenerationResultSaver resultSaver;
    @Mock private ArticleStatusAggregator aggregator;
    @Mock private PipelineMetrics metrics;

    private PipelineProperties pipelineProperties;
    private SubJobWorker worker;

    private ArticleGenerationJob job;
    private ArticleGenerationSubJob subJob;
    private UUID subJobId;
    private UUID articleId;

    @BeforeEach
    void setUp() {
        pipelineProperties = new PipelineProperties(3, 10, 3000, 5, null, false);
        worker = new SubJobWorker(lockService, subJobRepository, articleRepository, promptBuilder,
                primaryLlmClient, responseParser, validator,
                resultSaver, aggregator, pipelineProperties, metrics, new ObjectMapper());

        articleId = UUID.randomUUID();
        job = new ArticleGenerationJob(articleId, JobStatus.PENDING);
        setJobId(job, UUID.randomUUID());

        subJob = new ArticleGenerationSubJob(job, DifficultyLevel.EASY, JobStatus.PENDING);
        subJobId = UUID.randomUUID();
        setField(subJob, "id", subJobId);
    }

    // ── Setup helpers ─────────────────────────────────────────────────────────

    /**
     * Stubs the 3-step pipeline (content → vocabulary → quiz) for a short article.
     * Uses anyString() / any(Map.class) to match the 2-arg generate(prompt, schema) call.
     */
    private void stubThreeStepPipeline() {
        var step1Result = new GenerationResult("generated content", List.of(), List.of(), List.of(), null);
        var vocab = new GenerationResult.VocabularyData("word", "def", "example");
        var step2Result = new GenerationResult(null, List.of(), List.of(vocab), List.of(), null);
        var quiz = new GenerationResult.QuizData(QuizType.MULTIPLE_CHOICE, "q?",
                new com.curiofeed.backend.domain.model.QuizOptions(null, null), "A", "exp");
        var step3Result = new GenerationResult(null, List.of(), List.of(), List.of(quiz), null);

        when(promptBuilder.buildContentPrompt(anyString(), any())).thenReturn("p1");
        when(promptBuilder.buildVocabularyPrompt(anyString(), any())).thenReturn("p2");
        when(promptBuilder.buildQuizPrompt(anyString(), anyString())).thenReturn("p3");

        when(primaryLlmClient.generate(eq("p1"), any(Map.class))).thenReturn("r1");
        when(primaryLlmClient.generate(eq("p2"), any(Map.class))).thenReturn("r2");
        when(primaryLlmClient.generate(eq("p3"), isNull())).thenReturn("r3");

        when(responseParser.parse(eq("r1"), eq(GenerationResult.class))).thenReturn(step1Result);
        when(responseParser.parse(eq("r2"), eq(GenerationResult.class))).thenReturn(step2Result);
        when(responseParser.parse(eq("r3"), eq(GenerationResult.class))).thenReturn(step3Result);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("3단계 파이프라인 통과: status COMPLETED, retryCount=1, aggregate 호출")
    void process_pipelinePasses_completed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findByIdWithJob(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        stubThreeStepPipeline();

        var finalResult = new GenerationResult("generated content", List.of(),
                List.of(new GenerationResult.VocabularyData("word", "def", "ex")),
                List.of(new GenerationResult.QuizData(QuizType.MULTIPLE_CHOICE, "q?",
                        new com.curiofeed.backend.domain.model.QuizOptions(null, null), "A", "exp")), null);
        when(validator.validate(any())).thenReturn(ValidationResult.pass(0.85));
        when(resultSaver.save(any(), any(), any())).thenReturn(SaveStatus.FULL_SUCCESS);

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(subJob.getRetryCount()).isEqualTo(1);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("LLM 호출 실패: FAILED")
    void process_llmFails_failed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findByIdWithJob(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.buildContentPrompt(anyString(), any())).thenReturn("p1");
        when(primaryLlmClient.generate(eq("p1"), any(Map.class)))
                .thenThrow(new LlmClientException("timeout"));

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(subJob.getRetryCount()).isEqualTo(1);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("LLM 응답 파싱 실패: FAILED")
    void process_parseFails_failed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findByIdWithJob(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.buildContentPrompt(anyString(), any())).thenReturn("p1");
        when(primaryLlmClient.generate(eq("p1"), any(Map.class))).thenReturn("bad");
        when(responseParser.parse(eq("bad"), eq(GenerationResult.class)))
                .thenThrow(new LlmParseException("bad json"));

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.FAILED);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("tryLock false: retryCount 증가 없음, 이후 단계 호출 없음")
    void process_lockFails_noProgress() {
        when(lockService.tryLock(subJobId)).thenReturn(false);

        worker.process(subJobId);

        assertThat(subJob.getRetryCount()).isEqualTo(0);
        verifyNoInteractions(promptBuilder, primaryLlmClient, responseParser, resultSaver, aggregator);
    }

    @Test
    @DisplayName("maxRetryCount 초과: LLM 호출 없이 FAILED 처리")
    void process_exceedsMaxRetry_failsEarly() {
        setField(subJob, "retryCount", 3);
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findByIdWithJob(subJobId)).thenReturn(Optional.of(subJob));

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.FAILED);
        verifyNoInteractions(primaryLlmClient);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("heartbeat executor가 finally에서 반드시 종료된다 (LLM 예외 시에도)")
    void process_heartbeatAlwaysStopped() {
        AtomicBoolean heartbeatStopped = new AtomicBoolean(false);
        SubJobWorker spyWorker = new SubJobWorker(
                lockService, subJobRepository, articleRepository, promptBuilder,
                primaryLlmClient, responseParser, validator,
                resultSaver, aggregator, pipelineProperties, metrics, new ObjectMapper()) {
            @Override
            protected void stopHeartbeat(java.util.concurrent.ScheduledExecutorService executor) {
                heartbeatStopped.set(true);
                super.stopHeartbeat(executor);
            }
        };

        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findByIdWithJob(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.buildContentPrompt(anyString(), any())).thenReturn("p1");
        when(primaryLlmClient.generate(eq("p1"), any(Map.class)))
                .thenThrow(new LlmClientException("fail"));

        spyWorker.process(subJobId);

        assertThat(heartbeatStopped.get()).isTrue();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Article mockArticle(UUID id) {
        Article article = newInstance(Article.class);
        setField(article, "id", id);
        setField(article, "originalContent", "original content");
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

    private void setJobId(ArticleGenerationJob job, UUID id) {
        setField(job, "id", id);
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
