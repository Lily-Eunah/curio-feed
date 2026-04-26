package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findByArticleContentIdOrderByCreatedAtAsc(UUID articleContentId);

    @Modifying
    @Query("DELETE FROM Quiz q WHERE q.articleContent.id = :contentId")
    void deleteAllByArticleContentId(@Param("contentId") UUID contentId);
}
