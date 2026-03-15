package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.Article;

import java.util.Optional;
import java.util.UUID;

public interface ArticleDetailRepository {
    // Top-down: define the required interface for the service.
    // The actual JPA implementation will be handled in Step 3.
    Optional<Article> findPublishedByIdWithContentsVocabsAndQuizzes(UUID id);
}
