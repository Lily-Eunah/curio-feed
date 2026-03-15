package com.curiofeed.backend.api.dto;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record ArticleDetailResponse(
    UUID id,
    String title,
    String originalTitle,
    String sourceName,
    String sourceUrl,
    String thumbnailUrl,
    Instant publishedAt,
    String categoryName,
    List<DifficultyLevel> availableLevels,
    ArticleContentDto content
) {
    @Builder
    public record ArticleContentDto(
        DifficultyLevel level,
        String content,
        String audioUrl,
        List<VocabularyDto> vocabularies,
        List<QuizDto> quizzes
    ) {}

    @Builder
    public record VocabularyDto(
        String word,
        String definition,
        String exampleSentence
    ) {}

    @Builder
    public record QuizDto(
        String type,
        String question,
        List<String> options,
        String answer,
        String explanation
    ) {}
}
