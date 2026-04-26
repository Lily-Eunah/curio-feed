package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleContentRepository extends JpaRepository<ArticleContent, UUID> {
    Optional<ArticleContent> findByArticleIdAndLevel(UUID articleId, DifficultyLevel level);
}
