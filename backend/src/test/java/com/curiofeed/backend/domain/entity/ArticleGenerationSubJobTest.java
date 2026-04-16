package com.curiofeed.backend.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleGenerationSubJobTest {

    private ArticleGenerationSubJob createSubJobWithStatus(JobStatus status) throws Exception {
        ArticleGenerationSubJob subJob = createInstance(ArticleGenerationSubJob.class);
        setField(subJob, "status", status);
        return subJob;
    }

    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<T> clazz) throws Exception {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    // ── 허용된 전이 3가지 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING → PROCESSING 전이 허용")
    void pendingToProcessing() throws Exception {
        ArticleGenerationSubJob subJob = createSubJobWithStatus(JobStatus.PENDING);
        subJob.updateStatus(JobStatus.PROCESSING);
        assertThat(subJob.getStatus()).isEqualTo(JobStatus.PROCESSING);
    }

    @Test
    @DisplayName("PROCESSING → COMPLETED 전이 허용")
    void processingToCompleted() throws Exception {
        ArticleGenerationSubJob subJob = createSubJobWithStatus(JobStatus.PROCESSING);
        subJob.updateStatus(JobStatus.COMPLETED);
        assertThat(subJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    @DisplayName("PROCESSING → FAILED 전이 허용")
    void processingToFailed() throws Exception {
        ArticleGenerationSubJob subJob = createSubJobWithStatus(JobStatus.PROCESSING);
        subJob.updateStatus(JobStatus.FAILED);
        assertThat(subJob.getStatus()).isEqualTo(JobStatus.FAILED);
    }

    // ── 불허 전이: terminal 상태 변경 시도 ──────────────────────────────────────

    @Test
    @DisplayName("COMPLETED 이후 전이 불가 — IllegalStateException")
    void completedIsTerminal() throws Exception {
        ArticleGenerationSubJob subJob = createSubJobWithStatus(JobStatus.COMPLETED);
        assertThatThrownBy(() -> subJob.updateStatus(JobStatus.PENDING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from COMPLETED to PENDING");
    }

    @Test
    @DisplayName("FAILED 이후 전이 불가 — IllegalStateException")
    void failedIsTerminal() throws Exception {
        ArticleGenerationSubJob subJob = createSubJobWithStatus(JobStatus.FAILED);
        assertThatThrownBy(() -> subJob.updateStatus(JobStatus.PROCESSING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from FAILED to PROCESSING");
    }

    // ── 불허 전이: 동일 상태로의 전이 ────────────────────────────────────────────

    @Test
    @DisplayName("PENDING → PENDING 동일 상태 전이 불가")
    void sameStateTransitionPending() throws Exception {
        ArticleGenerationSubJob subJob = createSubJobWithStatus(JobStatus.PENDING);
        assertThatThrownBy(() -> subJob.updateStatus(JobStatus.PENDING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from PENDING to PENDING");
    }

    @Test
    @DisplayName("PROCESSING → PROCESSING 동일 상태 전이 불가")
    void sameStateTransitionProcessing() throws Exception {
        ArticleGenerationSubJob subJob = createSubJobWithStatus(JobStatus.PROCESSING);
        assertThatThrownBy(() -> subJob.updateStatus(JobStatus.PROCESSING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from PROCESSING to PROCESSING");
    }
}
