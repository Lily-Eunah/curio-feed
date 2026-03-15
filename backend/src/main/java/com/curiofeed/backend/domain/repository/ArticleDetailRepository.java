package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ArticleDetailRepository extends JpaRepository<Article, UUID> {

    @Query("SELECT DISTINCT a FROM Article a " +
           "JOIN FETCH a.category " +
           "LEFT JOIN FETCH a.contents " +
           "WHERE a.id = :id AND a.status = 'PUBLISHED'")
    Optional<Article> findPublishedByIdWithContentsVocabsAndQuizzes(@Param("id") UUID id);
}
