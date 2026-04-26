package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.Article;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    // Retrieve active articles (Soft delete support: ignore HIDDEN)
    Page<Article> findAllByStatusNot(ArticleStatus status, Pageable pageable);
    
    // Retrieve only explicitly PUBLISHED articles
    Page<Article> findAllByStatus(ArticleStatus status, Pageable pageable);

    // Retrieve an article by slug unless it's hidden
    Optional<Article> findBySlugAndStatusNot(String slug, ArticleStatus status);

    Optional<Article> findBySourceUrl(String sourceUrl);

    boolean existsBySlug(String slug);
}
