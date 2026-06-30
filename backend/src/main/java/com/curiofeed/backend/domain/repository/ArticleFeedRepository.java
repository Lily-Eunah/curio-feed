package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.api.dto.ArticleFeedResponse;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ArticleFeedRepository extends JpaRepository<com.curiofeed.backend.domain.entity.Article, UUID> {

    @Query("""
        SELECT new com.curiofeed.backend.api.dto.ArticleFeedResponse(
            CAST(a.id AS string), 
            a.title, 
            COALESCE(ac.content, a.originalContent),
            c.displayName, 
            a.sourcePublisher,
            a.publishedAt,
            0
        )
        FROM Article a
        JOIN a.category c
        LEFT JOIN ArticleContent ac ON ac.article = a AND ac.level = :level
        WHERE a.status = :status
        AND (a.publishedAt, a.id) < (:cursorAt, :cursorId)
        ORDER BY a.publishedAt DESC, a.id DESC
    """)
    List<ArticleFeedResponse> findFeedByCursor(
        @Param("status") ArticleStatus status,
        @Param("cursorAt") Instant cursorAt,
        @Param("cursorId") UUID cursorId,
        @Param("level") com.curiofeed.backend.domain.entity.DifficultyLevel level,
        Pageable pageable
    );

    @Query("""
        SELECT new com.curiofeed.backend.api.dto.ArticleFeedResponse(
            CAST(a.id AS string), 
            a.title, 
            COALESCE(ac.content, a.originalContent),
            c.displayName,
            a.sourcePublisher,
            a.publishedAt,
            0
        )
        FROM Article a
        JOIN a.category c
        LEFT JOIN ArticleContent ac ON ac.article = a AND ac.level = :level
        WHERE a.status = :status
        ORDER BY a.publishedAt DESC, a.id DESC
    """)
    List<ArticleFeedResponse> findFeedFirstPage(
        @Param("status") ArticleStatus status,
        @Param("level") com.curiofeed.backend.domain.entity.DifficultyLevel level,
        Pageable pageable
    );
}
