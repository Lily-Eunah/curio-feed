package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.ArticleGenerationStepJob;
import com.curiofeed.backend.domain.entity.GenerationStepType;
import com.curiofeed.backend.domain.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleGenerationStepJobRepository extends JpaRepository<ArticleGenerationStepJob, UUID> {

    List<ArticleGenerationStepJob> findBySubJobIdOrderByStepType(UUID subJobId);

    Optional<ArticleGenerationStepJob> findBySubJobIdAndStepType(UUID subJobId, GenerationStepType stepType);

    void deleteBySubJobIdAndStepType(UUID subJobId, GenerationStepType stepType);

    void deleteBySubJobId(UUID subJobId);

    @Modifying
    @Query("""
            UPDATE ArticleGenerationStepJob s
            SET s.status = 'PROCESSING', s.startedAt = :now, s.attemptCount = s.attemptCount + 1
            WHERE s.id = :id AND s.status = 'PENDING'
            """)
    int tryLockStep(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE ArticleGenerationStepJob s
            SET s.lastHeartbeatAt = :ts
            WHERE s.id = :id
            """)
    void updateHeartbeat(@Param("id") UUID id, @Param("ts") Instant ts);

    boolean existsBySubJobIdAndStepTypeAndStatus(UUID subJobId, GenerationStepType stepType, JobStatus status);
}
