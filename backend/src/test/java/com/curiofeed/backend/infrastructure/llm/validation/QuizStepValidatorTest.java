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
                new VocabularyData("optimistic", "hopeful — used when expecting a good outcome", "ex"),
                new VocabularyData("target", "to aim at — used when attacking a specific entity", "ex"),
                new VocabularyData("restrict", "to limit — used when movement is constrained", "ex"),
                new VocabularyData("escalate", "to worsen — used when conflict intensifies", "ex")
        );
    }

    private static QuizData comprehensionMcq(String question) {
        QuizOptions opts = new QuizOptions(List.of(
                new QuizChoice("A", "Hotels expected a large surge in visitors.", "correct"),
                new QuizChoice("B", "Bookings were extremely high.", "wrong"),
                new QuizChoice("C", "The event was cancelled.", "wrong"),
                new QuizChoice("D", "Prices dropped significantly.", "wrong")
        ), null);
        return new QuizData(QuizType.MULTIPLE_CHOICE, question, opts, "A", "explanation");
    }

    private static QuizData reasoningMcq(String question) {
        QuizOptions opts = new QuizOptions(List.of(
                new QuizChoice("A", "Hotels remained optimistic because they expected a late surge in bookings.", "correct"),
                new QuizChoice("B", "They reduced prices to attract more guests.", "wrong"),
                new QuizChoice("C", "The government guaranteed full occupancy.", "wrong"),
                new QuizChoice("D", "Most travellers booked months in advance.", "wrong")
        ), null);
        return new QuizData(QuizType.MULTIPLE_CHOICE, question, opts, "A", "explanation");
    }

    private static QuizData passageGroundedShortAnswer(String question, String modelAnswer) {
        return new QuizData(QuizType.SHORT_ANSWER, question, new QuizOptions(null, null), modelAnswer, "Target word: optimistic");
    }

    // ── Happy path ───────────────────────────────────────────────────────────────

    @Test
    void validQuizSet_noHardFails() {
        List<QuizData> quizzes = List.of(
                comprehensionMcq("What best summarizes the main concern described in the article?"),
                reasoningMcq("Why did hotel owners remain optimistic despite weak early bookings?"),
                passageGroundedShortAnswer(
                        "In one sentence, explain why hotel owners stayed hopeful despite weak early bookings. Use the word 'optimistic' in your answer.",
                        "Hotel owners remained optimistic because they believed a late surge in World Cup visitors would fill their rooms.")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isFalse();
    }

    // ── Hard fails ────────────────────────────────────────────────────────────────

    @Test
    void wrongQuizCount_hardFail() {
        List<QuizData> quizzes = List.of(
                comprehensionMcq("What is the main concern?"),
                reasoningMcq("Why did bookings stay low?")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors.get(0)).contains("expected 3 quizzes");
    }

    @Test
    void q1IsShortAnswer_hardFail() {
        List<QuizData> quizzes = List.of(
                passageGroundedShortAnswer("What caused the prices to rise?", "Prices surged due to high demand."),
                reasoningMcq("Why did hotel owners stay optimistic?"),
                passageGroundedShortAnswer("Explain using 'optimistic'.", "They remained optimistic about the future.")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors).anyMatch(e -> e.contains("quiz[0] must be MULTIPLE_CHOICE"));
    }

    @Test
    void q3IsMultipleChoice_hardFail() {
        List<QuizData> quizzes = List.of(
                comprehensionMcq("What best summarizes the article?"),
                reasoningMcq("Why did bookings remain weak?"),
                comprehensionMcq("What sentence best describes the main situation?")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors).anyMatch(e -> e.contains("quiz[2] must be SHORT_ANSWER"));
    }

    @Test
    void mcqBlankCorrectAnswer_hardFail() {
        QuizOptions opts = new QuizOptions(List.of(
                new QuizChoice("A", "Text A", "expl"),
                new QuizChoice("B", "Text B", "expl"),
                new QuizChoice("C", "Text C", "expl"),
                new QuizChoice("D", "Text D", "expl")
        ), null);
        QuizData q1 = new QuizData(QuizType.MULTIPLE_CHOICE, "Q?", opts, "", "exp");
        List<QuizData> quizzes = List.of(
                q1,
                reasoningMcq("Why did bookings stay low?"),
                passageGroundedShortAnswer("Use 'optimistic'.", "They remained optimistic.")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors).anyMatch(e -> e.contains("quiz[0].correctAnswer is blank"));
    }

    @Test
    void mcqCorrectAnswerNotABCD_hardFail() {
        QuizOptions opts = new QuizOptions(List.of(
                new QuizChoice("A", "Text A", "expl"),
                new QuizChoice("B", "Text B", "expl"),
                new QuizChoice("C", "Text C", "expl"),
                new QuizChoice("D", "Text D", "expl")
        ), null);
        QuizData q1 = new QuizData(QuizType.MULTIPLE_CHOICE, "Q?", opts, "E", "exp");
        List<QuizData> quizzes = List.of(
                q1,
                reasoningMcq("Why did bookings stay low?"),
                passageGroundedShortAnswer("Use 'optimistic'.", "They remained optimistic.")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors).anyMatch(e -> e.contains("correctAnswer 'E' is not A/B/C/D"));
    }

    // ── Soft warnings ─────────────────────────────────────────────────────────────

    @Test
    void shallowQ1_softWarn() {
        List<QuizData> quizzes = List.of(
                comprehensionMcq("What percentage of hotels reported losses?"),
                reasoningMcq("Why did hotel owners remain optimistic?"),
                passageGroundedShortAnswer("Use 'optimistic'.", "They remained optimistic about future bookings.")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]") && e.contains("shallow factual-lookup"));
    }

    @Test
    void q2VocabDefinitionQuestion_softWarn() {
        List<QuizData> quizzes = List.of(
                comprehensionMcq("What best summarizes the article?"),
                reasoningMcq("Which sentence uses the word 'surge' correctly in a school context?"),
                passageGroundedShortAnswer("Use 'optimistic'.", "They remained optimistic.")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]") && e.contains("vocabulary-definition question"));
    }

    @Test
    void q3ModelAnswerMissingVocab_softWarn() {
        List<QuizData> quizzes = List.of(
                comprehensionMcq("What best summarizes the article?"),
                reasoningMcq("Why did bookings stay low?"),
                passageGroundedShortAnswer(
                        "In one sentence, explain why hotel owners stayed hopeful. Use the word 'optimistic' in your answer.",
                        "Hotel owners stayed hopeful because they believed more visitors would arrive later.")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]") && e.contains("does not contain any vocab word"));
    }

    @Test
    void q3QuestionMissingVocabWord_softWarn() {
        List<QuizData> quizzes = List.of(
                comprehensionMcq("What best summarizes the article?"),
                reasoningMcq("Why did bookings stay low?"),
                passageGroundedShortAnswer(
                        "In one sentence, explain why hotel owners stayed hopeful despite weak early bookings.",
                        "Hotel owners remained optimistic because a late surge in tourists was expected.")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]") && e.contains("does not mention a vocab word"));
    }

    @Test
    void validPassageGroundedQ3_noVocabWarnings() {
        List<QuizData> quizzes = List.of(
                comprehensionMcq("What best summarizes the main concern?"),
                reasoningMcq("Why did hotel owners remain hopeful?"),
                passageGroundedShortAnswer(
                        "In one sentence, explain why hotel owners stayed hopeful despite weak early bookings. Use the word 'optimistic' in your answer.",
                        "Hotel owners remained optimistic because they believed a late surge in World Cup visitors would fill their rooms.")
        );
        List<String> errors = validator.validate(quizzes, vocabs());
        assertThat(errors).noneMatch(e -> e.contains("does not contain any vocab word"));
        assertThat(errors).noneMatch(e -> e.contains("does not mention a vocab word"));
    }
}
