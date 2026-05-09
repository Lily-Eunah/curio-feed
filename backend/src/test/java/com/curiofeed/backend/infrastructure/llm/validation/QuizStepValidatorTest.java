package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.entity.QuizType;
import com.curiofeed.backend.domain.model.GenerationResult.QuizData;
import com.curiofeed.backend.domain.model.GenerationResult.VocabularyData;
import com.curiofeed.backend.domain.model.QuizOptions;
import com.curiofeed.backend.domain.model.QuizChoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizStepValidatorTest {

    private QuizStepValidator validator;

    @BeforeEach
    void setUp() {
        validator = new QuizStepValidator();
    }

    private static List<VocabularyData> vocabs() {
        return List.of(
                new VocabularyData("surge", "sharp rise — used when prices increase rapidly", "ex"),
                new VocabularyData("blockade", "to block — used when access is cut off", "ex"),
                new VocabularyData("target", "to aim at — used when attacking a specific entity", "ex"),
                new VocabularyData("restrict", "to limit — used when movement is constrained", "ex"),
                new VocabularyData("escalate", "to worsen — used when conflict intensifies", "ex")
        );
    }

    private static QuizData mcq(String question, String vocabWord) {
        QuizOptions opts = new QuizOptions(List.of(
                new QuizChoice("A", "The price began to " + vocabWord + " when demand rose.", "correct"),
                new QuizChoice("B", "He tried to " + vocabWord + " the meeting to Thursday.", "wrong"),
                new QuizChoice("C", "She decided to " + vocabWord + " her lunch order.", "wrong"),
                new QuizChoice("D", "They refused to " + vocabWord + " the situation.", "wrong")
        ), null);
        return new QuizData(QuizType.MULTIPLE_CHOICE, question, opts, "A", "explanation");
    }

    private static QuizData shortAnswer(String question, String answer) {
        return new QuizData(QuizType.SHORT_ANSWER, question, new QuizOptions(null, null), answer, "explanation");
    }

    @Test
    void validQuizSet_noHardFails() {
        List<QuizData> quizzes = List.of(
                mcq("Why did oil prices rise sharply after the closure?", "surge"),
                mcq("Which sentence uses the word 'surge' correctly in a cooking context?", "surge"),
                shortAnswer("The athlete's performance began to ___ when training increased.", "surge")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isFalse();
    }

    @Test
    void wrongQuizCount_hardFail() {
        List<QuizData> quizzes = List.of(
                mcq("Why did prices rise?", "surge"),
                mcq("Which uses surge correctly?", "surge")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors.get(0)).contains("expected 3 quizzes");
    }

    @Test
    void q1IsShortAnswer_hardFail() {
        List<QuizData> quizzes = List.of(
                shortAnswer("What caused the prices to rise?", "surge"),
                mcq("Which uses surge correctly?", "surge"),
                shortAnswer("Fill: The price began to ___ sharply.", "surge")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors).anyMatch(e -> e.contains("quiz[0] must be MULTIPLE_CHOICE"));
    }

    @Test
    void q3IsMultipleChoice_hardFail() {
        List<QuizData> quizzes = List.of(
                mcq("Why did prices rise?", "surge"),
                mcq("Which uses surge correctly?", "surge"),
                mcq("What sentence uses blockade correctly?", "blockade") // wrong type for Q3
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors).anyMatch(e -> e.contains("quiz[2] must be SHORT_ANSWER"));
    }

    @Test
    void q3AnswerNotInVocab_softWarn() {
        List<QuizData> quizzes = List.of(
                mcq("Why did prices rise?", "surge"),
                mcq("Which uses surge correctly?", "surge"),
                shortAnswer("Fill: The price began to ___ sharply.", "blend") // not in vocab
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]") && e.contains("not found in vocab list"));
    }

    @Test
    void shallowQ1_softWarn() {
        List<QuizData> quizzes = List.of(
                mcq("What percentage of oil passes through the strait?", "surge"), // shallow
                mcq("Which uses surge correctly?", "surge"),
                shortAnswer("The price began to ___.", "surge")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]") && e.contains("shallow factual-lookup"));
    }

    @Test
    void q2WithVocabInChoices_passes() {
        // Verify Q2 uses a vocab word in choices
        QuizOptions opts = new QuizOptions(List.of(
                new QuizChoice("A", "The price began to surge when demand rose.", "correct"),
                new QuizChoice("B", "He tried to surge the meeting.", "wrong"),
                new QuizChoice("C", "She surged her lunch order.", "wrong"),
                new QuizChoice("D", "They surged the situation.", "wrong")
        ), null);
        QuizData q2 = new QuizData(QuizType.MULTIPLE_CHOICE,
                "Which sentence uses 'surge' correctly in a school context?", opts, "A", "expl");

        List<QuizData> quizzes = List.of(
                mcq("Why did prices rise?", "surge"),
                q2,
                shortAnswer("The economy began to ___.", "surge")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(errors).noneMatch(e -> e.contains("Q2") && e.contains("vocab word"));
    }
}
