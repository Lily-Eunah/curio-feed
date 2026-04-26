package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.QuizType;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.domain.model.GenerationResult.QuizData;
import com.curiofeed.backend.domain.model.GenerationResult.VocabularyData;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Content(0.3) + Vocabulary(0.3) + Quiz(0.4) 가중 합산으로 0~1 품질 점수 계산.
 */
@Component
public class QualityScorer {

    public double score(GenerationResult result) {
        double contentScore = scoreContent(result.content());
        double vocabScore = scoreVocabulary(result);
        double quizScore = scoreQuiz(result);
        return contentScore * 0.3 + vocabScore * 0.3 + quizScore * 0.4;
    }

    private double scoreContent(String content) {
        double lengthScore = Math.min(1.0, content.length() / 800.0);
        double sentenceScore = Math.min(1.0, countSentences(content) / 8.0);
        return (lengthScore + sentenceScore) / 2.0;
    }

    private double scoreVocabulary(GenerationResult result) {
        if (result.vocabularies() == null || result.vocabularies().isEmpty()) return 0.0;

        List<VocabularyData> vocabs = result.vocabularies();
        String contentLower = result.content().toLowerCase();

        double inContentScore = (double) vocabs.stream()
                .filter(v -> contentLower.contains(v.word().toLowerCase()))
                .count() / vocabs.size();

        double defScore = vocabs.stream()
                .mapToDouble(v -> Math.min(1.0, v.definition().length() / 40.0))
                .average()
                .orElse(0.0);

        double uniqueScore = (double) vocabs.stream()
                .map(v -> v.word().toLowerCase())
                .distinct()
                .count() / vocabs.size();

        return inContentScore * 0.5 + defScore * 0.3 + uniqueScore * 0.2;
    }

    private double scoreQuiz(GenerationResult result) {
        if (result.quizzes() == null || result.quizzes().isEmpty()) return 0.0;

        List<QuizData> quizzes = result.quizzes();

        long mcqCount = quizzes.stream().filter(q -> q.type() == QuizType.MULTIPLE_CHOICE).count();
        long saCount = quizzes.stream().filter(q -> q.type() == QuizType.SHORT_ANSWER).count();
        double typeScore = (mcqCount == 2 && saCount == 1) ? 1.0 : 0.0;

        double choicesScore = mcqCount == 0 ? 0.0 : (double) quizzes.stream()
                .filter(q -> q.type() == QuizType.MULTIPLE_CHOICE)
                .filter(q -> q.options() != null
                        && q.options().getChoices() != null
                        && q.options().getChoices().size() == 4)
                .count() / mcqCount;

        double explanationScore = mcqCount == 0 ? 0.0 : (double) quizzes.stream()
                .filter(q -> q.type() == QuizType.MULTIPLE_CHOICE)
                .filter(q -> q.explanation() != null && !q.explanation().isBlank())
                .count() / mcqCount;

        double saScore = quizzes.stream()
                .filter(q -> q.type() == QuizType.SHORT_ANSWER)
                .mapToDouble(q -> {
                    int words = q.correctAnswer().trim().split("\\s+").length;
                    if (words <= 3) return 1.0;
                    if (words <= 5) return 0.6;
                    return 0.2;
                })
                .average()
                .orElse(1.0);

        return typeScore * 0.3 + choicesScore * 0.3 + explanationScore * 0.2 + saScore * 0.2;
    }

    private int countSentences(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == '.' || c == '!' || c == '?') count++;
        }
        return Math.max(count, 1);
    }
}
