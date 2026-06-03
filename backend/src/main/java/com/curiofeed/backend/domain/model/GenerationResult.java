package com.curiofeed.backend.domain.model;

import com.curiofeed.backend.domain.entity.QuizType;

import java.util.List;

public record GenerationResult(
        String content,
        List<String> candidates,
        List<VocabularyData> vocabularies,
        List<QuizData> quizzes,
        SourceDigestData sourceDigest
) {

    public boolean hasContent() {
        return content != null && !content.isBlank();
    }

    public boolean hasVocabularies() {
        return vocabularies != null && !vocabularies.isEmpty();
    }

    public boolean hasQuizzes() {
        return quizzes != null && !quizzes.isEmpty();
    }

    public boolean hasSourceDigest() {
        return sourceDigest != null && sourceDigest.centralStory() != null;
    }

    public record SourceDigestData(
            String centralStory,
            List<String> coreFacts,
            List<String> supportingDetails,
            List<String> omittedDetails
    ) {}

    public record VocabularyData(
            String word,
            String definition,
            String exampleSentence
    ) {}

    public record QuizData(
            QuizType type,
            String question,
            QuizOptions options,
            String correctAnswer,
            String explanation
    ) {}
}
