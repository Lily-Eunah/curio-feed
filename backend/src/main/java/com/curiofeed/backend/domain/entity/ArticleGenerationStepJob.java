package com.curiofeed.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "article_generation_step_jobs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_step_job_sub_job_step",
        columnNames = {"sub_job_id", "step_type"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleGenerationStepJob extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_job_id", nullable = false)
    private ArticleGenerationSubJob subJob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationStepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private int attemptCount = 0;

    private Instant startedAt;
    private Instant completedAt;
    private Instant lastHeartbeatAt;

    /** PASS or FAIL — null if not yet validated. */
    @Column(length = 20)
    private String validationStatus;

    @Column(columnDefinition = "TEXT")
    private String validationErrors;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** TIMEOUT, TRANSACTION, VALIDATION, PARSE, etc. */
    @Column(length = 50)
    private String failureReason;

    public static ArticleGenerationStepJob pending(ArticleGenerationSubJob subJob, GenerationStepType stepType) {
        var step = new ArticleGenerationStepJob();
        step.subJob = subJob;
        step.stepType = stepType;
        step.status = JobStatus.PENDING;
        step.attemptCount = 0;
        return step;
    }

    public void markProcessing() {
        this.status = JobStatus.PROCESSING;
        this.startedAt = Instant.now();
        this.attemptCount++;
        this.errorMessage = null;
        this.validationStatus = null;
        this.validationErrors = null;
    }

    public void markCompleted(String validationStatus) {
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.validationStatus = validationStatus;
    }

    public void markSkipped(String reason) {
        this.status = JobStatus.SKIPPED;
        this.completedAt = Instant.now();
        this.validationStatus = "SKIPPED";
        this.errorMessage = reason;
    }

    public void markFailed(String errorMessage, String validationErrors, String failureReason) {
        this.status = JobStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
        this.validationErrors = validationErrors;
        this.failureReason = failureReason;
    }

    public void resetToPending() {
        this.status = JobStatus.PENDING;
        this.startedAt = null;
        this.completedAt = null;
        this.lastHeartbeatAt = null;
        this.errorMessage = null;
        this.validationStatus = null;
        this.validationErrors = null;
        this.failureReason = null;
    }

    public void updateHeartbeat(Instant ts) {
        this.lastHeartbeatAt = ts;
    }

    public boolean isCompleted() {
        return this.status == JobStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == JobStatus.FAILED;
    }

    public boolean isPending() {
        return this.status == JobStatus.PENDING;
    }
}
