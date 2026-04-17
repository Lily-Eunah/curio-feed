package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.ArticleGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleGenerationJobRepository extends JpaRepository<ArticleGenerationJob, UUID> {

    Optional<ArticleGenerationJob> findByArticleId(UUID articleId);

    /**
     * 모든 SubJob이 terminal state이고 Article.status가 DRAFT 또는 FAILED인 Job 조회.
     * (Worker 처리 중 집계 실패로 상태가 안 바뀐 케이스 복구용)
     */
    @Query("""
            SELECT j FROM ArticleGenerationJob j
            WHERE NOT EXISTS (
                SELECT s FROM ArticleGenerationSubJob s
                WHERE s.job.id = j.id
                  AND s.status IN ('PENDING', 'PROCESSING')
            )
            AND EXISTS (
                SELECT a FROM Article a
                WHERE a.id = j.articleId
                  AND a.status IN ('DRAFT', 'FAILED')
            )
            """)
    List<ArticleGenerationJob> findJobsPendingReconciliation();
}
