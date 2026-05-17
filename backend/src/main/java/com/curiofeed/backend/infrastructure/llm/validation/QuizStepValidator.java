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
 *   - MCQ correctAnswer must be A/B/C/D and must not be blank
 *   - Q3 options is empty
 *
 * Soft warnings:
 *   - Q1 appears to be a shallow factual-lookup
 *   - Q2 appears to be a vocabulary-definition question (not passage reasoning)
 *   - Q3 correctAnswer (model answer) does not contain a vocab word
 *   - Q3 question does not reference the article or a vocab word
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

    // Patterns suggesting Q2 is a vocab-definition MCQ rather than passage reasoning
    private static final Pattern VOCAB_DEFINITION_PATTERN = Pattern.compile(
        "(?i)which\\s+sentence\\s+uses|" +
        "(?i)which\\s+word\\s+fits|" +
        "(?i)which\\s+option\\s+correctly\\s+uses|" +
        "(?i)choose\\s+the\\s+sentence\\s+that\\s+(best\\s+)?uses"
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

        // Q1 — must be MULTIPLE_CHOICE, passage comprehension
        QuizData q1 = quizzes.get(0);
        if (q1.type() != QuizType.MULTIPLE_CHOICE) {
            errors.add("quiz[0] must be MULTIPLE_CHOICE, got " + q1.type());
        } else {
            validateMcqChoices(q1, 0, errors);
            if (q1.question() != null && SHALLOW_QUESTION_PATTERN.matcher(q1.question()).find()) {
                errors.add("[SOFT] quiz[0] appears to be a shallow factual-lookup question: " + q1.question());
            }
        }

        // Q2 — must be MULTIPLE_CHOICE, passage reasoning
        QuizData q2 = quizzes.get(1);
        if (q2.type() != QuizType.MULTIPLE_CHOICE) {
            errors.add("quiz[1] must be MULTIPLE_CHOICE, got " + q2.type());
        } else {
            validateMcqChoices(q2, 1, errors);
            if (q2.question() != null && VOCAB_DEFINITION_PATTERN.matcher(q2.question()).find()) {
                errors.add("[SOFT] quiz[1] (Q2) appears to be a vocabulary-definition question, not passage reasoning: " + q2.question());
            }
        }

        // Q3 — must be SHORT_ANSWER; correctAnswer is a model-answer sentence containing a vocab word
        QuizData q3 = quizzes.get(2);
        if (q3.type() != QuizType.SHORT_ANSWER) {
            errors.add("quiz[2] must be SHORT_ANSWER, got " + q3.type());
        } else {
            boolean optionsEmpty = q3.options() == null
                    || (q3.options().getChoices() == null || q3.options().getChoices().isEmpty());
            if (!optionsEmpty) {
                errors.add("[SOFT] quiz[2] SHORT_ANSWER options should be empty but contains data");
            }
            // Model answer (correctAnswer) should contain a vocab word
            if (q3.correctAnswer() != null && !vocabWords.isEmpty()) {
                String answerLower = q3.correctAnswer().toLowerCase(Locale.ROOT);
                boolean containsVocab = vocabWords.stream().anyMatch(answerLower::contains);
                if (!containsVocab) {
                    errors.add("[SOFT] quiz[2] correctAnswer does not contain any vocab word — " +
                               "model answer should use the target vocabulary word");
                }
            }
            // Question should explicitly name the target vocab word (soft check)
            if (q3.question() != null && !vocabWords.isEmpty()) {
                String questionLower = q3.question().toLowerCase(Locale.ROOT);
                boolean questionMentionsVocab = vocabWords.stream().anyMatch(questionLower::contains);
                if (!questionMentionsVocab) {
                    errors.add("[SOFT] quiz[2] question does not mention a vocab word — " +
                               "Q3 should ask the learner to use a specific vocabulary word");
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
