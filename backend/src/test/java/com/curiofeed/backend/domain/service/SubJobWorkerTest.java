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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
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
    @Mock private LlmClient fallbackLlmClient;
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
        pipelineProperties = new PipelineProperties(3, 10, 3000, 5, null);
        worker = new SubJobWorker(lockService, subJobRepository, articleRepository, promptBuilder,
                primaryLlmClient, fallbackLlmClient, responseParser, validator,
                resultSaver, aggregator, pipelineProperties, metrics);

        articleId = UUID.randomUUID();
        job = new ArticleGenerationJob(articleId, JobStatus.PENDING);
        setJobId(job, UUID.randomUUID());

        subJob = new ArticleGenerationSubJob(job, DifficultyLevel.EASY, JobStatus.PENDING);
        subJobId = UUID.randomUUID();
        setField(subJob, "id", subJobId);
    }

    @Test
    @DisplayName("primary 통과: status COMPLETED, retryCount=1, aggregate 호출")
    void process_primaryPasses_completed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.build(any(), any())).thenReturn("prompt");

        var result = new GenerationResult("content", List.of(), List.of());
        when(primaryLlmClient.generate("prompt")).thenReturn("{}");
        when(responseParser.parse(eq("{}"), eq(GenerationResult.class))).thenReturn(result);
        when(validator.validate(result)).thenReturn(ValidationResult.pass(0.85));
        when(resultSaver.save(any(), any(), any())).thenReturn(SaveStatus.FULL_SUCCESS);

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(subJob.getRetryCount()).isEqualTo(1);
        verify(aggregator).aggregate(job.getId());
        verifyNoInteractions(fallbackLlmClient);
    }

    @Test
    @DisplayName("primary score 미달 → fallback 통과: COMPLETED, fallback 사용")
    void process_primaryLowScore_fallbackPasses_completed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.build(any(), any())).thenReturn("prompt");

        var primaryResult = new GenerationResult("short", List.of(), List.of());
        var fallbackResult = new GenerationResult("long content", List.of(), List.of());

        when(primaryLlmClient.generate("prompt")).thenReturn("primary");
        when(responseParser.parse(eq("primary"), eq(GenerationResult.class))).thenReturn(primaryResult);
        when(validator.validate(primaryResult)).thenReturn(ValidationResult.pass(0.50)); // threshold 0.7 미달

        when(fallbackLlmClient.generate("prompt")).thenReturn("fallback");
        when(responseParser.parse(eq("fallback"), eq(GenerationResult.class))).thenReturn(fallbackResult);
        when(validator.validate(fallbackResult)).thenReturn(ValidationResult.pass(0.80));
        when(resultSaver.save(any(), any(), any())).thenReturn(SaveStatus.FULL_SUCCESS);

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        verify(metrics).recordFallback(DifficultyLevel.EASY);
        verify(metrics).recordQwenUsage();
    }

    @Test
    @DisplayName("primary hard fail → fallback 통과: COMPLETED")
    void process_primaryHardFail_fallbackPasses_completed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.build(any(), any())).thenReturn("prompt");

        var primaryResult = new GenerationResult("content", List.of(), List.of());
        var fallbackResult = new GenerationResult("good content", List.of(), List.of());

        when(primaryLlmClient.generate("prompt")).thenReturn("primary");
        when(responseParser.parse(eq("primary"), eq(GenerationResult.class))).thenReturn(primaryResult);
        when(validator.validate(primaryResult))
                .thenReturn(ValidationResult.fail(List.of("vocab count != 5"), 0.0));

        when(fallbackLlmClient.generate("prompt")).thenReturn("fallback");
        when(responseParser.parse(eq("fallback"), eq(GenerationResult.class))).thenReturn(fallbackResult);
        when(validator.validate(fallbackResult)).thenReturn(ValidationResult.pass(0.82));
        when(resultSaver.save(any(), any(), any())).thenReturn(SaveStatus.FULL_SUCCESS);

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        verify(metrics).recordFallback(DifficultyLevel.EASY);
    }

    @Test
    @DisplayName("primary + fallback 모두 실패: FAILED")
    void process_primaryAndFallbackFail_failed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.build(any(), any())).thenReturn("prompt");
        when(primaryLlmClient.generate("prompt")).thenThrow(new LlmClientException("timeout"));
        when(fallbackLlmClient.generate("prompt")).thenThrow(new LlmClientException("timeout"));

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(subJob.getRetryCount()).isEqualTo(1);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("primary + fallback 모두 파싱 실패: FAILED")
    void process_primaryAndFallbackParseFail_failed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.build(any(), any())).thenReturn("prompt");
        when(primaryLlmClient.generate("prompt")).thenReturn("bad");
        when(responseParser.parse(eq("bad"), eq(GenerationResult.class)))
                .thenThrow(new LlmParseException("bad json"));
        when(fallbackLlmClient.generate("prompt")).thenReturn("bad");

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
        verifyNoInteractions(promptBuilder, primaryLlmClient, fallbackLlmClient, responseParser, resultSaver, aggregator);
    }

    @Test
    @DisplayName("maxRetryCount 초과: LLM 호출 없이 FAILED 처리")
    void process_exceedsMaxRetry_failsEarly() {
        setField(subJob, "retryCount", 3);
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.FAILED);
        verifyNoInteractions(primaryLlmClient, fallbackLlmClient);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("heartbeat executor가 finally에서 반드시 종료된다 (LLM 예외 시에도)")
    void process_heartbeatAlwaysStopped() {
        AtomicBoolean heartbeatStopped = new AtomicBoolean(false);
        SubJobWorker spyWorker = new SubJobWorker(
                lockService, subJobRepository, articleRepository, promptBuilder,
                primaryLlmClient, fallbackLlmClient, responseParser, validator,
                resultSaver, aggregator, pipelineProperties, metrics) {
            @Override
            protected void stopHeartbeat(java.util.concurrent.ScheduledExecutorService executor) {
                heartbeatStopped.set(true);
                super.stopHeartbeat(executor);
            }
        };

        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.build(any(), any())).thenReturn("prompt");
        when(primaryLlmClient.generate("prompt")).thenThrow(new LlmClientException("fail"));
        when(fallbackLlmClient.generate("prompt")).thenThrow(new LlmClientException("fail"));

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
