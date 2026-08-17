package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleContentRepository extends JpaRepository<ArticleContent, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"vocabularies"})
    Optional<ArticleContent> findByArticleIdAndLevel(UUID articleId, DifficultyLevel level);

    /**
     * How many difficulty levels of an article actually hold text.
     *
     * <p>A row can exist with empty content — a retry clears it that way — so counting rows
     * would overstate readiness. The feed falls back to the source article when a level is
     * missing, which makes "how many levels are really filled" a safety question, not a
     * cosmetic one.
     */
    @Query("SELECT COUNT(c) FROM ArticleContent c WHERE c.article.id = :articleId AND TRIM(c.content) <> ''")
    long countPopulatedLevels(@Param("articleId") UUID articleId);

    @Query("SELECT c.article.id, COUNT(c) FROM ArticleContent c "
            + "WHERE c.article.id IN :articleIds AND TRIM(c.content) <> '' GROUP BY c.article.id")
    List<Object[]> countPopulatedLevelsByArticleIds(@Param("articleIds") List<UUID> articleIds);
}
