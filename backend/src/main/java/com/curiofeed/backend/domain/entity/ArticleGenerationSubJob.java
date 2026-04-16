package com.curiofeed.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "article_generation_sub_jobs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_subjob_job_level",
        columnNames = {"job_id", "level"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleGenerationSubJob extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ArticleGenerationJob job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private int retryCount = 0;

    private Instant lastHeartbeatAt;
    
    public ArticleGenerationSubJob(ArticleGenerationJob job, DifficultyLevel level, JobStatus status) {
        this.job = job;
        this.level = level;
        this.status = status;
        this.retryCount = 0;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void updateStatus(JobStatus newStatus) {
        if (this.status == newStatus) {
            throw new IllegalStateException(
                    "Cannot transition from " + this.status + " to " + newStatus);
        }
        if (this.status == JobStatus.COMPLETED || this.status == JobStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public void updateHeartbeat(Instant heartbeat) {
        this.lastHeartbeatAt = heartbeat;
    }
}
