package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.model.GenerationResult.VocabularyData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VocabStepValidatorTest {

    private VocabStepValidator validator;

    @BeforeEach
    void setUp() {
        validator = new VocabStepValidator(new VocabLemmatizer());
    }

    private static VocabularyData vocab(String word) {
        return new VocabularyData(word,
                "to " + word + " something — used when a situation requires it",
                "The teacher decided to " + word + " the schedule for the week.");
    }

    private static final String CONTENT =
            "Iran targeted vessels and enforced a blockade of the strait. " +
            "Tensions escalated sharply as global oil prices surged above $100. " +
            "The IRGC asserted its right to restrict passage.";

    @Test
    void allWordsInContent_passes() {
        List<VocabularyData> vocabs = List.of(
                vocab("target"),
                vocab("blockade"),
                vocab("escalate"),
                vocab("surge"),
                vocab("restrict")
        );
        List<String> errors = validator.validate(vocabs, CONTENT);
        assertThat(validator.isHardFail(errors)).isFalse();
        // Only soft warnings (definition format) are possible
        assertThat(errors).noneMatch(e -> e.contains("not found in content"));
    }

    @Test
    void wordNotInContent_hardFail() {
        List<VocabularyData> vocabs = List.of(
                vocab("target"),
                vocab("blockade"),
                vocab("escalate"),
                vocab("surge"),
                new VocabularyData("depreciate",
                        "to reduce in value — used when an asset loses worth",
                        "The car depreciated after the first year.")
        );
        // "depreciate" not in CONTENT
        List<String> errors = validator.validate(vocabs, CONTENT);
        assertThat(errors).anyMatch(e -> e.contains("not found in content") && e.contains("depreciate"));
        assertThat(validator.isHardFail(errors)).isTrue();
    }

    @Test
    void inflectedFormInContent_passes() {
        // "targeted" in content → base form "target" should pass
        assertThat(validator.validate(
                List.of(vocab("target"), vocab("blockade"), vocab("escalate"), vocab("surge"), vocab("restrict")),
                CONTENT
        )).noneMatch(e -> e.contains("not found") && e.contains("target"));
    }

    @Test
    void wrongCount_hardFail() {
        List<VocabularyData> only4 = List.of(vocab("target"), vocab("blockade"), vocab("escalate"), vocab("surge"));
        List<String> errors = validator.validate(only4, CONTENT);
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors.get(0)).contains("expected 5 vocabularies");
    }

    @Test
    void forbiddenWord_hardFail() {
        List<VocabularyData> vocabs = List.of(
                vocab("target"),
                vocab("blockade"),
                vocab("escalate"),
                vocab("surge"),
                vocab("thing")  // "thing" is forbidden
        );
        // "thing" is in CONTENT only if content mentions it — it doesn't here
        // but the forbidden check fires first for the word itself
        List<String> errors = validator.validate(vocabs, CONTENT);
        assertThat(errors).anyMatch(e -> e.contains("forbidden word") && e.contains("thing"));
    }

    @Test
    void blankWord_hardFail() {
        List<VocabularyData> vocabs = List.of(
                new VocabularyData("", "some definition — used when needed", "example sentence."),
                vocab("blockade"), vocab("escalate"), vocab("surge"), vocab("restrict")
        );
        List<String> errors = validator.validate(vocabs, CONTENT);
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors).anyMatch(e -> e.contains("word is blank"));
    }

    @Test
    void missingUsedWhenInDefinition_softWarn() {
        List<VocabularyData> vocabs = List.of(
                new VocabularyData("target", "to aim at something", "The coach targeted the issue."),
                vocab("blockade"), vocab("escalate"), vocab("surge"), vocab("restrict")
        );
        List<String> errors = validator.validate(vocabs, CONTENT);
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]") && e.contains("used when"));
        assertThat(validator.isHardFail(errors)).isFalse();
    }
}
