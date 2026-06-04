package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.Vocabulary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, UUID> {

    @Modifying
    @Query("DELETE FROM Vocabulary v WHERE v.articleContent.id = :contentId")
    void deleteAllByArticleContentId(@Param("contentId") UUID contentId);

    @Query("""
        SELECT v.word FROM Vocabulary v
        WHERE v.articleContent.level = :level
          AND v.articleContent.article.id != :currentArticleId
        ORDER BY v.articleContent.createdAt DESC
        """)
    List<String> findRecentWordsByLevel(
        @Param("level") DifficultyLevel level,
        @Param("currentArticleId") UUID currentArticleId,
        Pageable pageable
    );
}
