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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubJobWorkerTest {

    @Mock private SubJobLockService lockService;
    @Mock private ArticleGenerationSubJobRepository subJobRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private ArticlePromptBuilder promptBuilder;
    @Mock private LlmClient llmClient;
    @Mock private LlmResponseParser responseParser;
    @Mock private GenerationResultSaver resultSaver;
    @Mock private ArticleStatusAggregator aggregator;

    private PipelineProperties pipelineProperties;
    private SubJobWorker worker;

    private ArticleGenerationJob job;
    private ArticleGenerationSubJob subJob;
    private UUID subJobId;
    private UUID articleId;

    @BeforeEach
    void setUp() {
        pipelineProperties = new PipelineProperties(3, 10, 3000, 5);
        worker = new SubJobWorker(lockService, subJobRepository, articleRepository, promptBuilder,
                llmClient, responseParser, resultSaver, aggregator, pipelineProperties);

        articleId = UUID.randomUUID();
        job = new ArticleGenerationJob(articleId, JobStatus.PENDING);
        setJobId(job, UUID.randomUUID());

        subJob = new ArticleGenerationSubJob(job, DifficultyLevel.EASY, JobStatus.PENDING);
        subJobId = UUID.randomUUID();
        setField(subJob, "id", subJobId);
    }

    @Test
    @DisplayName("정상 처리: status COMPLETED, retryCount=1, aggregate 호출")
    void process_success() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));
        Article article = mockArticle(articleId);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(promptBuilder.build(any(), any())).thenReturn("prompt");
        when(llmClient.generate("prompt")).thenReturn("{}");

        var fullResult = new GenerationResult("content", List.of(), List.of());
        when(responseParser.parse(eq("{}"), eq(GenerationResult.class))).thenReturn(fullResult);
        when(resultSaver.save(any(), any(), any())).thenReturn(SaveStatus.FULL_SUCCESS);

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(subJob.getRetryCount()).isEqualTo(1);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("tryLock false: retryCount 증가 없음, 이후 단계 호출 없음")
    void process_lockFails_noProgress() {
        when(lockService.tryLock(subJobId)).thenReturn(false);

        worker.process(subJobId);

        assertThat(subJob.getRetryCount()).isEqualTo(0);
        verifyNoInteractions(promptBuilder, llmClient, responseParser, resultSaver, aggregator);
    }

    @Test
    @DisplayName("LLM 호출 실패: status FAILED, retryCount=1, aggregate 호출")
    void process_llmFails_statusFailed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.build(any(), any())).thenReturn("prompt");
        when(llmClient.generate("prompt")).thenThrow(new LlmClientException("timeout"));

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(subJob.getRetryCount()).isEqualTo(1);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("파싱 실패: status FAILED, retryCount=1")
    void process_parseFails_statusFailed() {
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(mockArticle(articleId)));
        when(promptBuilder.build(any(), any())).thenReturn("prompt");
        when(llmClient.generate("prompt")).thenReturn("bad json");
        when(responseParser.parse(eq("bad json"), eq(GenerationResult.class)))
                .thenThrow(new LlmParseException("bad json"));

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(subJob.getRetryCount()).isEqualTo(1);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("maxRetryCount 초과: LLM 호출 없이 FAILED 처리")
    void process_exceedsMaxRetry_failsEarly() {
        // retryCount already at maxRetryCount (3)
        setField(subJob, "retryCount", 3);
        when(lockService.tryLock(subJobId)).thenReturn(true);
        when(subJobRepository.findById(subJobId)).thenReturn(Optional.of(subJob));

        worker.process(subJobId);

        assertThat(subJob.getStatus()).isEqualTo(JobStatus.FAILED);
        verifyNoInteractions(llmClient);
        verify(aggregator).aggregate(job.getId());
    }

    @Test
    @DisplayName("heartbeat executor가 finally에서 반드시 종료된다 (LLM 예외 시에도)")
    void process_heartbeatAlwaysStopped() {
        AtomicBoolean heartbeatStopped = new AtomicBoolean(false);
        SubJobWorker spyWorker = new SubJobWorker(lockService, subJobRepository, articleRepository, promptBuilder,
                llmClient, responseParser, resultSaver, aggregator, pipelineProperties) {
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
        when(llmClient.generate("prompt")).thenThrow(new LlmClientException("fail"));

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
