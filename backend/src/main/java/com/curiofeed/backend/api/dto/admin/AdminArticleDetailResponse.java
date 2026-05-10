package com.curiofeed.backend.api.dto.admin;

import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.model.QuizOptions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminArticleDetailResponse(
        UUID id,
        ArticleStatus status,
        String title,
        String originalTitle,
        String sourceName,
        String sourceUrl,
        String categoryId,
        String categoryName,
        String originalContent,
        Instant createdAt,
        Instant publishedAt,
        JobInfo job,
        List<ContentInfo> contents
) {
    public record JobInfo(UUID jobId, String status) {}

    public record ContentInfo(
            UUID id,
            DifficultyLevel level,
            String content,
            String audioUrl,
            List<VocabInfo> vocabularies,
            List<QuizInfo> quizzes
    ) {}

    public record VocabInfo(
            UUID id,
            String word,
            String definition,
            String exampleSentence
    ) {}

    public record QuizInfo(
            UUID id,
            String question,
            String type,
            QuizOptions options,
            String correctAnswer,
            String explanation
    ) {}
}
