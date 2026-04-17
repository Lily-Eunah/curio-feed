package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleGenerationSubJobRepository extends JpaRepository<ArticleGenerationSubJob, UUID> {

    Optional<ArticleGenerationSubJob> findByJobIdAndLevel(UUID jobId, DifficultyLevel level);

    List<ArticleGenerationSubJob> findByJobId(UUID jobId);

    /**
     * PENDING → PROCESSING 원자적 상태 전이.
     * 상태 조건 UPDATE이므로 동시 요청이 와도 하나만 성공.
     * 반환값 1 = lock 성공, 0 = 이미 다른 상태 (실패).
     *
     * 주의: JPQL direct UPDATE는 상태 전이 검증을 우회하므로
     *       이 메서드 외의 상태 변경은 반드시 entity.updateStatus() 를 사용할 것.
     */
    @Modifying
    @Query("UPDATE ArticleGenerationSubJob s SET s.status = 'PROCESSING' WHERE s.id = :id AND s.status = 'PENDING'")
    int tryLockSubJob(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE ArticleGenerationSubJob s SET s.lastHeartbeatAt = :heartbeat WHERE s.id = :id")
    void updateHeartbeat(@Param("id") UUID id, @Param("heartbeat") Instant heartbeat);

    @Query("SELECT s FROM ArticleGenerationSubJob s WHERE s.status = 'PENDING' ORDER BY s.createdAt ASC")
    List<ArticleGenerationSubJob> findPendingJobs(org.springframework.data.domain.Pageable pageable);

    /**
     * Admin API 전용: FAILED SubJob을 PENDING으로 재설정, retryCount=0으로 초기화.
     */
    @Modifying
    @Query("UPDATE ArticleGenerationSubJob s SET s.status = 'PENDING', s.retryCount = 0 WHERE s.id = :id")
    void resetToPendingWithRetryReset(@Param("id") UUID id);

    /**
     * ReconciliationScheduler 전용: stale SubJob을 PENDING으로 복구.
     * JPQL direct UPDATE 예외 허용 케이스 (stale 복구, 상태 검증 우회 의도적).
     */
    @Modifying
    @Query("UPDATE ArticleGenerationSubJob s SET s.status = 'PENDING' WHERE s.id = :id AND s.status = 'PROCESSING'")
    void resetToPending(@Param("id") UUID id);

    /**
     * ReconciliationScheduler 전용: FAILED 강제 설정.
     */
    @Modifying
    @Query("UPDATE ArticleGenerationSubJob s SET s.status = :status WHERE s.id = :id")
    void forceSetStatus(@Param("id") UUID id, @Param("status") JobStatus status);

    /**
     * Stale PROCESSING SubJob 조회:
     * - lastHeartbeatAt IS NULL AND createdAt < threshold (heartbeat 시작 전 서버 장애)
     * - OR lastHeartbeatAt < threshold (heartbeat 중단)
     */
    @Query("""
            SELECT s FROM ArticleGenerationSubJob s
            WHERE s.status = 'PROCESSING'
              AND ((s.lastHeartbeatAt IS NULL AND s.createdAt < :threshold)
                   OR s.lastHeartbeatAt < :threshold)
            """)
    List<ArticleGenerationSubJob> findStaleProcessingJobs(@Param("threshold") Instant threshold);
}
