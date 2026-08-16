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
            String suggestedTitle,
            String centralStory,
            List<String> coreFacts,
            List<String> supportingDetails,
            List<HumanDetail> humanDetails,
            List<String> omittedDetails
    ) {}

    /**
     * A concrete, person-level detail carried through to the article so it does not read as
     * pure statistics. Restated by the digest in its own words — never the source's phrasing.
     */
    public record HumanDetail(String who, String what) {}

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
