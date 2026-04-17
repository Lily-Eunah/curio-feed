package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.config.PipelineProperties;
import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubJobSchedulerTest {

    @Mock private ArticleGenerationSubJobRepository subJobRepository;
    @Mock private SubJobWorker subJobWorker;

    private PipelineProperties pipelineProperties;
    private SubJobScheduler scheduler;

    @BeforeEach
    void setUp() {
        pipelineProperties = new PipelineProperties(3, 10, 3000, 5);
        // SyncTaskExecutor: 테스트에서 동기 실행 (비동기 없음)
        scheduler = new SubJobScheduler(subJobRepository, subJobWorker, new SyncTaskExecutor(), pipelineProperties);
    }

    private ArticleGenerationSubJob pendingSubJob(UUID id) {
        ArticleGenerationJob job = new ArticleGenerationJob(UUID.randomUUID(), JobStatus.PENDING);
        setField(job, "id", UUID.randomUUID());
        ArticleGenerationSubJob subJob = new ArticleGenerationSubJob(job, DifficultyLevel.EASY, JobStatus.PENDING);
        setField(subJob, "id", id);
        return subJob;
    }

    @Test
    @DisplayName("PENDING SubJob 3개 → Worker.process 3번 호출")
    void processPending_threeSubJobs_callsWorkerThreeTimes() {
        UUID id1 = UUID.randomUUID(), id2 = UUID.randomUUID(), id3 = UUID.randomUUID();
        var subJobs = List.of(pendingSubJob(id1), pendingSubJob(id2), pendingSubJob(id3));
        when(subJobRepository.findPendingJobs(any())).thenReturn(subJobs);

        scheduler.processPending();

        verify(subJobWorker).process(id1);
        verify(subJobWorker).process(id2);
        verify(subJobWorker).process(id3);
    }

    @Test
    @DisplayName("batchSize=2 설정 시 2개만 조회 요청")
    void processPending_respectsBatchSize() {
        pipelineProperties = new PipelineProperties(3, 10, 3000, 2);
        scheduler = new SubJobScheduler(subJobRepository, subJobWorker, new SyncTaskExecutor(), pipelineProperties);

        when(subJobRepository.findPendingJobs(any())).thenReturn(List.of());

        scheduler.processPending();

        verify(subJobRepository).findPendingJobs(eq(PageRequest.of(0, 2)));
    }

    @Test
    @DisplayName("Worker 내부 예외 발생 시 나머지 SubJob 계속 처리")
    void processPending_workerException_continuesProcessing() {
        UUID id1 = UUID.randomUUID(), id2 = UUID.randomUUID();
        var subJobs = List.of(pendingSubJob(id1), pendingSubJob(id2));
        when(subJobRepository.findPendingJobs(any())).thenReturn(subJobs);

        doThrow(new RuntimeException("Worker failed")).when(subJobWorker).process(id1);

        scheduler.processPending();

        verify(subJobWorker).process(id1);
        verify(subJobWorker).process(id2);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
