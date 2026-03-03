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
            a.thumbnailUrl, 
            c.displayName, 
            a.sourceName, 
            a.publishedAt, 
            0
        )
        FROM Article a
        JOIN a.category c
        WHERE a.status = :status 
        AND (a.publishedAt < :cursorAt OR (a.publishedAt = :cursorAt AND a.id < :cursorId))
        ORDER BY a.publishedAt DESC, a.id DESC
    """)
    List<ArticleFeedResponse> findFeedByCursor(
        @Param("status") ArticleStatus status,
        @Param("cursorAt") Instant cursorAt,
        @Param("cursorId") UUID cursorId,
        Pageable pageable
    );

    @Query("""
        SELECT new com.curiofeed.backend.api.dto.ArticleFeedResponse(
            CAST(a.id AS string), 
            a.title, 
            a.thumbnailUrl, 
            c.displayName, 
            a.sourceName, 
            a.publishedAt,
            0
        )
        FROM Article a
        JOIN a.category c
        WHERE a.status = :status 
        ORDER BY a.publishedAt DESC, a.id DESC
    """)
    List<ArticleFeedResponse> findFeedFirstPage(
        @Param("status") ArticleStatus status,
        Pageable pageable
    );
}
