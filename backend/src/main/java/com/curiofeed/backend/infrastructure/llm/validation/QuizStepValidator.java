package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.entity.QuizType;
import com.curiofeed.backend.domain.model.GenerationResult.QuizData;
import com.curiofeed.backend.domain.model.GenerationResult.VocabularyData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates Step 3 (quiz-only) output.
 *
 * Hard gates:
 *   - Exactly 3 quizzes (Q1=MCQ, Q2=MCQ, Q3=SHORT_ANSWER)
 *   - Each MCQ has exactly 4 choices
 *   - Q3 options is empty object
 *
 * Soft warnings:
 *   - Q1 appears to be a shallow factual-lookup
 *   - Q2 does not reference a vocab word
 *   - Q3 answer not in vocab list
 */
@Component
public class QuizStepValidator {

    private static final int REQUIRED_QUIZ_COUNT = 3;
    private static final int REQUIRED_MCQ_CHOICES = 4;

    private static final Pattern SHALLOW_QUESTION_PATTERN = Pattern.compile(
        "(?i)what\\s+(percentage|percent|year|was\\s+the\\s+price|was\\s+the\\s+number)|" +
        "(?i)how\\s+many|" +
        "(?i)which\\s+country|" +
        "(?i)when\\s+did|" +
        "(?i)what\\s+date"
    );

    public List<String> validate(List<QuizData> quizzes, List<VocabularyData> vocabs) {
        List<String> errors = new ArrayList<>();

        if (quizzes == null) {
            errors.add("quizzes is null");
            return errors;
        }
        if (quizzes.size() != REQUIRED_QUIZ_COUNT) {
            errors.add("expected " + REQUIRED_QUIZ_COUNT + " quizzes, got " + quizzes.size());
            return errors;
        }

        List<String> vocabWords = vocabs == null ? List.of()
                : vocabs.stream().map(v -> v.word().toLowerCase(Locale.ROOT)).toList();

        // Q1 — must be MULTIPLE_CHOICE, comprehension
        QuizData q1 = quizzes.get(0);
        if (q1.type() != QuizType.MULTIPLE_CHOICE) {
            errors.add("quiz[0] must be MULTIPLE_CHOICE, got " + q1.type());
        } else {
            validateMcqChoices(q1, 0, errors);
            if (q1.question() != null && SHALLOW_QUESTION_PATTERN.matcher(q1.question()).find()) {
                errors.add("[SOFT] quiz[0] appears to be a shallow factual-lookup question: " + q1.question());
            }
        }

        // Q2 — must be MULTIPLE_CHOICE, vocabulary application
        QuizData q2 = quizzes.get(1);
        if (q2.type() != QuizType.MULTIPLE_CHOICE) {
            errors.add("quiz[1] must be MULTIPLE_CHOICE, got " + q2.type());
        } else {
            validateMcqChoices(q2, 1, errors);
            // Q2 should reference a vocab word in its choices
            boolean q2UsesVocab = false;
            if (q2.options() != null && q2.options().getChoices() != null) {
                q2UsesVocab = q2.options().getChoices().stream()
                        .anyMatch(c -> c.getText() != null && vocabWords.stream()
                                .anyMatch(w -> c.getText().toLowerCase(Locale.ROOT).contains(w)));
            }
            if (!q2UsesVocab) {
                errors.add("[SOFT] quiz[1] (Q2) choices do not appear to contain a vocab word — may not be vocabulary-application");
            }
        }

        // Q3 — must be SHORT_ANSWER, answer must be in vocab list
        QuizData q3 = quizzes.get(2);
        if (q3.type() != QuizType.SHORT_ANSWER) {
            errors.add("quiz[2] must be SHORT_ANSWER, got " + q3.type());
        } else {
            // options must be empty
            boolean optionsEmpty = q3.options() == null
                    || (q3.options().getChoices() == null || q3.options().getChoices().isEmpty());
            if (!optionsEmpty) {
                errors.add("[SOFT] quiz[2] SHORT_ANSWER options should be empty but contains data");
            }
            // answer must be in vocab list
            if (q3.correctAnswer() != null) {
                String answer = q3.correctAnswer().toLowerCase(Locale.ROOT).trim();
                boolean inVocab = vocabWords.contains(answer);
                if (!inVocab) {
                    errors.add("[SOFT] quiz[2] correctAnswer '" + q3.correctAnswer() + "' not found in vocab list " + vocabWords);
                }
            }
        }

        // All quizzes: correctAnswer and question must be present
        for (int i = 0; i < quizzes.size(); i++) {
            QuizData q = quizzes.get(i);
            if (isBlank(q.question())) errors.add("quiz[" + i + "].question is blank");
            if (isBlank(q.correctAnswer())) errors.add("quiz[" + i + "].correctAnswer is blank");
        }

        return errors;
    }

    private void validateMcqChoices(QuizData quiz, int index, List<String> errors) {
        boolean hasChoices = quiz.options() != null
                && quiz.options().getChoices() != null
                && !quiz.options().getChoices().isEmpty();
        if (!hasChoices) {
            errors.add("quiz[" + index + "] MCQ missing choices");
            return;
        }
        int count = quiz.options().getChoices().size();
        if (count != REQUIRED_MCQ_CHOICES) {
            errors.add("[SOFT] quiz[" + index + "] MCQ has " + count + " choices (expected " + REQUIRED_MCQ_CHOICES + ")");
        }
        Set<String> validKeys = Set.of("A", "B", "C", "D");
        for (var choice : quiz.options().getChoices()) {
            if (choice.getText() == null || choice.getText().isBlank()) {
                errors.add("[SOFT] quiz[" + index + "] MCQ choice has blank text");
            }
        }
        if (quiz.correctAnswer() != null && !validKeys.contains(quiz.correctAnswer())) {
            errors.add("quiz[" + index + "] correctAnswer '" + quiz.correctAnswer() + "' is not A/B/C/D");
        }
    }

    public boolean isHardFail(List<String> errors) {
        return errors.stream().anyMatch(e -> !e.startsWith("[SOFT]"));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
