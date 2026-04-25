package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.entity.QuizType;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.domain.model.GenerationResult.QuizData;
import com.curiofeed.backend.domain.model.GenerationResult.VocabularyData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class LearningValidator {

    private static final int REQUIRED_MCQ_COUNT = 2;
    private static final int REQUIRED_SA_COUNT = 1;
    private static final int REQUIRED_MCQ_CHOICES = 4;

    public List<String> check(GenerationResult result) {
        List<String> errors = new ArrayList<>();
        String contentLower = result.content().toLowerCase();

        checkVocabularies(result.vocabularies(), contentLower, errors);
        checkQuizzes(result.quizzes(), errors);

        return errors;
    }

    private void checkVocabularies(List<VocabularyData> vocabs, String contentLower, List<String> errors) {
        Set<String> seen = new HashSet<>();
        for (VocabularyData v : vocabs) {
            if (!contentLower.contains(v.word().toLowerCase())) {
                errors.add("[SOFT] vocab word not found in content: " + v.word());
            }
            if (!seen.add(v.word().toLowerCase())) {
                errors.add("[SOFT] duplicate vocab word: " + v.word());
            }
        }
    }

    private void checkQuizzes(List<QuizData> quizzes, List<String> errors) {
        long mcqCount = quizzes.stream().filter(q -> q.type() == QuizType.MULTIPLE_CHOICE).count();
        long saCount = quizzes.stream().filter(q -> q.type() == QuizType.SHORT_ANSWER).count();

        if (mcqCount != REQUIRED_MCQ_COUNT) {
            errors.add("expected " + REQUIRED_MCQ_COUNT + " MULTIPLE_CHOICE quizzes, got " + mcqCount);
        }
        if (saCount != REQUIRED_SA_COUNT) {
            errors.add("expected " + REQUIRED_SA_COUNT + " SHORT_ANSWER quiz, got " + saCount);
        }

        for (int i = 0; i < quizzes.size(); i++) {
            QuizData quiz = quizzes.get(i);
            if (quiz.type() == QuizType.MULTIPLE_CHOICE) {
                checkMcq(quiz, i, errors);
            }
            if (quiz.type() == QuizType.SHORT_ANSWER) {
                checkShortAnswer(quiz, i, errors);
            }
        }
    }

    private void checkMcq(QuizData quiz, int index, List<String> errors) {
        boolean hasChoices = quiz.options() != null
                && quiz.options().getChoices() != null
                && !quiz.options().getChoices().isEmpty();
        if (!hasChoices) {
            errors.add("quiz[" + index + "] MCQ missing choices");
            return;
        }
        int choiceCount = quiz.options().getChoices().size();
        if (choiceCount != REQUIRED_MCQ_CHOICES) {
            errors.add("[SOFT] quiz[" + index + "] MCQ has " + choiceCount + " choices (expected " + REQUIRED_MCQ_CHOICES + ")");
        }
        if (quiz.explanation() == null || quiz.explanation().isBlank()) {
            errors.add("[SOFT] quiz[" + index + "] MCQ missing explanation");
        }
    }

    private void checkShortAnswer(QuizData quiz, int index, List<String> errors) {
        int wordCount = quiz.correctAnswer().trim().split("\\s+").length;
        if (wordCount > 5) {
            errors.add("[SOFT] quiz[" + index + "] SHORT_ANSWER correctAnswer too long: " + wordCount + " words");
        }
    }

    public boolean isHardFail(List<String> errors) {
        return errors.stream().anyMatch(e -> !e.startsWith("[SOFT]"));
    }
}
