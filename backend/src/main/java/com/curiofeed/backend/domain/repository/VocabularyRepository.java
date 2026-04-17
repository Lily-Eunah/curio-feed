package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, UUID> {

    @Modifying
    @Query("DELETE FROM Vocabulary v WHERE v.articleContent.id = :contentId")
    void deleteAllByArticleContentId(@Param("contentId") UUID contentId);
}
