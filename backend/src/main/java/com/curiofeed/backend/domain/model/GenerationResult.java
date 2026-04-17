package com.curiofeed.backend.domain.model;

import com.curiofeed.backend.domain.entity.QuizType;

import java.util.List;

public record GenerationResult(
        String content,
        List<VocabularyData> vocabularies,
        List<QuizData> quizzes
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
